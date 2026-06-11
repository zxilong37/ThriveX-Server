package liuyuyang.net.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import liuyuyang.net.common.execption.CustomException;
import liuyuyang.net.common.utils.CommonUtils;
import liuyuyang.net.common.utils.JwtUtils;
import liuyuyang.net.dto.agent.AgentChatDTO;
import liuyuyang.net.dto.agent.DocumentGenerateDTO;
import liuyuyang.net.dto.agent.DocumentReviewDTO;
import liuyuyang.net.dto.agent.KnowledgeSearchDTO;
import liuyuyang.net.model.AgentMessage;
import liuyuyang.net.model.AgentSession;
import liuyuyang.net.model.AgentToolLog;
import liuyuyang.net.model.Assistant;
import liuyuyang.net.model.DocumentResult;
import liuyuyang.net.model.DocumentTask;
import liuyuyang.net.vo.PageVo;
import liuyuyang.net.vo.agent.AgentChatVO;
import liuyuyang.net.vo.agent.CitationVO;
import liuyuyang.net.vo.agent.DocumentReviewVO;
import liuyuyang.net.vo.agent.KnowledgeSearchResultVO;
import liuyuyang.net.web.mapper.AgentMessageMapper;
import liuyuyang.net.web.mapper.AgentSessionMapper;
import liuyuyang.net.web.mapper.AgentToolLogMapper;
import liuyuyang.net.web.mapper.AssistantMapper;
import liuyuyang.net.web.mapper.DocumentResultMapper;
import liuyuyang.net.web.mapper.DocumentTaskMapper;
import liuyuyang.net.web.service.AgentService;
import liuyuyang.net.web.service.KnowledgeService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional
public class AgentServiceImpl implements AgentService {
    private static final String INTENT_DOCUMENT = "DOCUMENT_GENERATION";
    private static final String INTENT_QA = "KNOWLEDGE_QA";
    private static final String INTENT_IMPORT = "KNOWLEDGE_IMPORT";

    @Resource
    private KnowledgeService knowledgeService;
    @Resource
    private AgentSessionMapper agentSessionMapper;
    @Resource
    private AgentMessageMapper agentMessageMapper;
    @Resource
    private DocumentTaskMapper documentTaskMapper;
    @Resource
    private DocumentResultMapper documentResultMapper;
    @Resource
    private AgentToolLogMapper agentToolLogMapper;
    @Resource
    private AssistantMapper assistantMapper;
    @Resource
    private CommonUtils commonUtils;
    @Resource
    private ObjectMapper objectMapper;

    @Override
    public AgentChatVO chat(AgentChatDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getMessage())) {
            throw new CustomException(400, "请输入问题或文档需求");
        }
        Integer userId = getCurrentUserId();
        String intent = classifyIntent(dto.getMessage(), dto.getMode(), dto.getDocType());
        AgentSession session = ensureSession(userId, dto.getSessionId(), dto.getMessage(), intent);
        saveMessage(session.getId(), userId, "user", intent, dto.getMessage(), null, null);

        if (INTENT_IMPORT.equals(intent)) {
            int count = knowledgeService.importDefaultSources().size();
            String answer = "已完成默认项目知识库导入，共处理 " + count + " 个知识来源。";
            saveMessage(session.getId(), userId, "assistant", intent, answer, null, null);
            AgentChatVO vo = new AgentChatVO();
            vo.setSessionId(session.getId());
            vo.setIntent(intent);
            vo.setAnswer(answer);
            vo.setCitations(new ArrayList<CitationVO>());
            return vo;
        }

        if (INTENT_DOCUMENT.equals(intent)) {
            DocumentGenerateDTO generateDTO = new DocumentGenerateDTO();
            generateDTO.setSessionId(session.getId());
            generateDTO.setPrompt(dto.getMessage());
            generateDTO.setDocType(StringUtils.hasText(dto.getDocType()) ? dto.getDocType() : "feature_design");
            generateDTO.setOutputFormat(StringUtils.hasText(dto.getOutputFormat()) ? dto.getOutputFormat() : "markdown");
            DocumentResult result = generateDocument(generateDTO);

            AgentChatVO vo = new AgentChatVO();
            vo.setSessionId(session.getId());
            vo.setIntent(intent);
            vo.setAnswer(result.getContent());
            vo.setResultId(result.getId());
            vo.setCitations(parseCitations(result.getCitations()));
            return vo;
        }

        AgentChatVO vo = answerQuestion(session, userId, dto.getMessage(), intent);
        touchSession(session);
        return vo;
    }

    @Override
    public DocumentResult generateDocument(DocumentGenerateDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getPrompt())) {
            throw new CustomException(400, "文档需求不能为空");
        }
        Integer userId = getCurrentUserId();
        AgentSession session = ensureSession(userId, dto.getSessionId(), dto.getPrompt(), INTENT_DOCUMENT);
        String docType = StringUtils.hasText(dto.getDocType()) ? dto.getDocType() : "feature_design";
        String title = StringUtils.hasText(dto.getTitle()) ? dto.getTitle() : defaultDocumentTitle(docType, dto.getPrompt());
        String now = nowMillis();

        DocumentTask task = new DocumentTask();
        task.setUserId(userId);
        task.setSessionId(session.getId());
        task.setTitle(title);
        task.setDocType(docType);
        task.setStatus("running");
        task.setPrompt(dto.getPrompt());
        task.setOutline(buildOutline(docType));
        task.setCreateTime(now);
        task.setUpdateTime(now);
        documentTaskMapper.insert(task);

        List<KnowledgeSearchResultVO> context = searchContext(dto.getPrompt() + " " + docType, 10, userId, task.getId());
        List<CitationVO> citations = toCitations(context);
        String citationJson = toJson(citations);
        task.setCitations(citationJson);

        String content = callDocumentModel(title, docType, dto.getPrompt(), context);
        if (!StringUtils.hasText(content)) {
            content = fallbackDocument(title, docType, dto.getPrompt(), context);
        }

        DocumentReviewVO review = reviewContent(content);
        DocumentResult result = new DocumentResult();
        result.setTaskId(task.getId());
        result.setUserId(userId);
        result.setTitle(title);
        result.setDocType(docType);
        result.setFormat(StringUtils.hasText(dto.getOutputFormat()) ? dto.getOutputFormat() : "markdown");
        result.setContent(content);
        result.setCitations(citationJson);
        result.setReviewScore(review.getScore());
        result.setReviewPassed(Boolean.TRUE.equals(review.getPassed()) ? 1 : 0);
        result.setReviewIssues(toJson(review.getIssues()));
        result.setCreateTime(nowMillis());
        result.setUpdateTime(nowMillis());
        documentResultMapper.insert(result);

        task.setStatus("completed");
        task.setUpdateTime(nowMillis());
        documentTaskMapper.updateById(task);
        saveMessage(session.getId(), userId, "assistant", INTENT_DOCUMENT, content, citationJson, result.getId());
        touchSession(session);
        return result;
    }

    @Override
    public DocumentReviewVO reviewDocument(DocumentReviewDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getContent())) {
            throw new CustomException(400, "审校内容不能为空");
        }
        return reviewContent(dto.getContent());
    }

    @Override
    public DocumentResult getDocument(Integer id) {
        DocumentResult result = documentResultMapper.selectById(id);
        if (result == null || !Objects.equals(result.getUserId(), getCurrentUserId())) {
            throw new CustomException(404, "文档不存在");
        }
        return result;
    }

    @Override
    public Page<DocumentResult> listDocuments(PageVo pageVo) {
        Integer userId = getCurrentUserId();
        LambdaQueryWrapper<DocumentResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentResult::getUserId, userId).orderByDesc(DocumentResult::getUpdateTime).orderByDesc(DocumentResult::getCreateTime);
        List<DocumentResult> list = documentResultMapper.selectList(wrapper);
        return commonUtils.getPageData(pageVo == null ? defaultPageVo() : pageVo, list);
    }

    @Override
    public Page<AgentSession> listSessions(PageVo pageVo) {
        Integer userId = getCurrentUserId();
        LambdaQueryWrapper<AgentSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentSession::getUserId, userId).orderByDesc(AgentSession::getUpdateTime).orderByDesc(AgentSession::getCreateTime);
        List<AgentSession> list = agentSessionMapper.selectList(wrapper);
        return commonUtils.getPageData(pageVo == null ? defaultPageVo() : pageVo, list);
    }

    private AgentChatVO answerQuestion(AgentSession session, Integer userId, String question, String intent) {
        List<KnowledgeSearchResultVO> context = searchContext(question, 8, userId, null);
        List<CitationVO> citations = toCitations(context);
        String citationJson = toJson(citations);
        String answer = callQaModel(question, context);
        if (!StringUtils.hasText(answer)) {
            answer = fallbackAnswer(question, context);
        }
        saveMessage(session.getId(), userId, "assistant", intent, answer, citationJson, null);

        AgentChatVO vo = new AgentChatVO();
        vo.setSessionId(session.getId());
        vo.setIntent(intent);
        vo.setAnswer(answer);
        vo.setCitations(citations);
        return vo;
    }

    private List<KnowledgeSearchResultVO> searchContext(String query, int limit, Integer userId, Integer taskId) {
        KnowledgeSearchDTO searchDTO = new KnowledgeSearchDTO();
        searchDTO.setQuery(query);
        searchDTO.setLimit(limit);
        try {
            List<KnowledgeSearchResultVO> results = knowledgeService.search(searchDTO);
            logTool(userId, taskId, "knowledge_search", query, "success", null);
            return results;
        } catch (Exception ex) {
            logTool(userId, taskId, "knowledge_search", query, "failed", ex.getMessage());
            throw ex;
        }
    }

    private String callQaModel(String question, List<KnowledgeSearchResultVO> context) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(message("system", "你是 ThriveX 项目的知识库问答智能体。只能基于给定知识库上下文回答，缺少依据时要明确说明。回答使用中文，尽量简洁，并保留可追溯性。"));
        messages.add(message("user", "问题：\n" + question + "\n\n知识库上下文：\n" + buildContext(context)));
        return callModel(messages, 1200);
    }

    private String callDocumentModel(String title, String docType, String prompt, List<KnowledgeSearchResultVO> context) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(message("system", "你是 ThriveX 的文档生成智能体。你必须基于知识库上下文生成结构化 Markdown 技术文档，不要写入真实密钥、密码、Token。缺少依据的内容用“待确认”标记。"));
        messages.add(message("user", "文档标题：" + title + "\n文档类型：" + docType + "\n用户需求：\n" + prompt + "\n\n推荐大纲：\n" + buildOutline(docType) + "\n\n知识库上下文：\n" + buildContext(context)));
        return callModel(messages, 2600);
    }

    private String callModel(List<Map<String, String>> messages, int maxTokens) {
        Assistant assistant = defaultAssistant();
        if (assistant == null || !StringUtils.hasText(assistant.getKey()) || assistant.getKey().trim().length() < 20 || assistant.getKey().trim().matches("x+")) {
            return null;
        }
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAcceptCharset(java.util.Collections.singletonList(StandardCharsets.UTF_8));
            headers.set("Authorization", "Bearer " + assistant.getKey().trim());

            Map<String, Object> body = new HashMap<>();
            body.put("model", assistant.getModel());
            body.put("messages", messages);
            body.put("temperature", 0.3);
            body.put("max_tokens", maxTokens);

            ResponseEntity<String> response = restTemplate.postForEntity(completionUrl(assistant.getUrl()), new HttpEntity<>(body, headers), String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || !StringUtils.hasText(content.asText())) {
                return null;
            }
            return content.asText();
        } catch (Exception ignored) {
            return null;
        }
    }

    private Assistant defaultAssistant() {
        LambdaQueryWrapper<Assistant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Assistant::getIsDefault, 1).last("limit 1");
        return assistantMapper.selectOne(wrapper);
    }

    private String completionUrl(String rawUrl) {
        String base = StringUtils.hasText(rawUrl) ? rawUrl.trim() : "https://api.deepseek.com";
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.endsWith("/chat/completions")) {
            return base;
        }
        return base + "/chat/completions";
    }

    private String fallbackAnswer(String question, List<KnowledgeSearchResultVO> context) {
        StringBuilder builder = new StringBuilder();
        builder.append("根据当前知识库，我对“").append(question).append("”的回答如下：\n\n");
        if (context == null || context.isEmpty()) {
            builder.append("暂未在知识库中检索到足够资料，无法给出确定结论。你可以先在知识库管理中导入项目文档、SQL 或接口说明后再提问。");
            return builder.toString();
        }
        builder.append("## 相关结论\n\n");
        for (KnowledgeSearchResultVO item : context) {
            builder.append("- **").append(item.getTitle()).append("**：")
                    .append(cleanLine(item.getContent(), 180)).append("\n");
        }
        builder.append("\n## 来源\n\n");
        for (KnowledgeSearchResultVO item : context) {
            builder.append("- ").append(item.getSourcePath()).append(" / ").append(item.getTitle()).append("\n");
        }
        return builder.toString();
    }

    private String fallbackDocument(String title, String docType, String prompt, List<KnowledgeSearchResultVO> context) {
        StringBuilder builder = new StringBuilder();
        builder.append("# ").append(title).append("\n\n");
        builder.append("## 目标\n\n");
        builder.append(prompt).append("\n\n");
        builder.append("## 文档类型\n\n");
        builder.append("- 类型：`").append(docType).append("`\n");
        builder.append("- 生成方式：基于 ThriveX 知识库检索结果生成\n\n");

        builder.append("## 生成依据\n\n");
        if (context == null || context.isEmpty()) {
            builder.append("当前知识库没有命中明确资料，以下内容仅保留规划框架，具体事实需要补充知识库后再审校。\n\n");
        } else {
            builder.append("| 来源 | 片段 |\n| --- | --- |\n");
            for (KnowledgeSearchResultVO item : context) {
                builder.append("| `").append(escapeTable(item.getSourcePath())).append("` | ")
                        .append(escapeTable(item.getTitle())).append(" |\n");
            }
            builder.append("\n");
        }

        builder.append("## 建议大纲\n\n");
        builder.append(buildOutline(docType)).append("\n");

        builder.append("## 关键资料摘要\n\n");
        if (context != null && !context.isEmpty()) {
            for (KnowledgeSearchResultVO item : context) {
                builder.append("### ").append(item.getTitle()).append("\n\n");
                builder.append(cleanLine(item.getContent(), 650)).append("\n\n");
                builder.append("来源：`").append(item.getSourcePath()).append("`\n\n");
            }
        } else {
            builder.append("- 待确认：需要补充知识库资料。\n\n");
        }

        builder.append("## 实施建议\n\n");
        builder.append("1. 先确认数据库、实体、Controller、Admin API 和页面类型是否一致。\n");
        builder.append("2. 后端新增接口时不要手写 `/api` 前缀，继续由 `WebConfig` 统一加前缀。\n");
        builder.append("3. 涉及公开接口时明确 `@NoTokenRequired`，需要防刷时沿用 `@RateLimit`。\n");
        builder.append("4. 前端只通过现有 API 封装和请求工具调用后端。\n");
        builder.append("5. 完成后执行构建检查和浏览器 QA。\n\n");

        builder.append("## 验收标准\n\n");
        builder.append("- 文档内容能够追溯到知识库来源。\n");
        builder.append("- 没有真实密钥、密码、Token 等敏感信息。\n");
        builder.append("- 与 ThriveX 三项目架构、接口路径和数据库契约一致。\n");
        builder.append("- 后台页面在 375、768、1440 宽度下可用。\n\n");

        builder.append("## 待确认事项\n\n");
        builder.append("- 如果某些章节没有知识库命中，需要继续导入项目资料或人工确认。\n");
        return builder.toString();
    }

    private DocumentReviewVO reviewContent(String content) {
        List<String> issues = new ArrayList<>();
        int score = 100;
        String lower = content == null ? "" : content.toLowerCase(Locale.ROOT);
        if (!StringUtils.hasText(content) || content.length() < 200) {
            issues.add("文档内容过短，可能没有覆盖完整需求。");
            score -= 25;
        }
        if (lower.contains("password=")
                || lower.contains("db_password=")
                || lower.contains("db_password:")
                || lower.contains("authorization: bearer ")
                || lower.contains("api_key=")
                || lower.contains("api-key:")
                || lower.contains("sk-")) {
            issues.add("文档可能包含敏感配置或密钥字段，需要人工复核。");
            score -= 30;
        }
        if (!content.contains("来源") && !content.contains("依据") && !content.contains("知识库")) {
            issues.add("文档缺少来源或生成依据说明。");
            score -= 15;
        }
        if (content.contains("待确认")) {
            issues.add("文档存在待确认内容，发布前需要补充依据。");
            score -= 8;
        }
        if (score < 0) {
            score = 0;
        }

        DocumentReviewVO vo = new DocumentReviewVO();
        vo.setScore(score);
        vo.setPassed(score >= 70);
        vo.setIssues(issues);
        return vo;
    }

    private String buildOutline(String docType) {
        String type = docType == null ? "" : docType;
        if ("deployment".equals(type)) {
            return "1. 项目组成\n2. 环境变量\n3. 数据库初始化\n4. Server 启动\n5. Blog 启动\n6. Admin 启动\n7. 常见问题\n";
        }
        if ("api".equals(type)) {
            return "1. 基础契约\n2. 鉴权方式\n3. 接口分组\n4. 请求与响应示例\n5. 错误码\n6. 调试方式\n";
        }
        if ("database".equals(type)) {
            return "1. 表结构来源\n2. 核心表说明\n3. 实体映射\n4. 初始化数据\n5. 变更注意事项\n";
        }
        if ("architecture".equals(type)) {
            return "1. 总体架构\n2. Blog 前台\n3. Admin 后台\n4. Server 后端\n5. 数据库契约\n6. 集成边界\n";
        }
        if ("testing".equals(type)) {
            return "1. 静态检查\n2. 后端构建\n3. 浏览器 QA\n4. 响应式矩阵\n5. 已知限制\n";
        }
        return "1. 背景与目标\n2. 功能范围\n3. 数据库设计\n4. 后端接口\n5. 前端页面\n6. 权限与安全\n7. 测试与验收\n";
    }

    private String defaultDocumentTitle(String docType, String prompt) {
        if ("deployment".equals(docType)) {
            return "ThriveX 启动部署文档";
        }
        if ("api".equals(docType)) {
            return "ThriveX 接口说明文档";
        }
        if ("database".equals(docType)) {
            return "ThriveX 数据库说明文档";
        }
        if ("architecture".equals(docType)) {
            return "ThriveX 架构设计文档";
        }
        if ("testing".equals(docType)) {
            return "ThriveX 测试验收文档";
        }
        String text = cleanLine(prompt, 24);
        return StringUtils.hasText(text) ? text + "设计文档" : "ThriveX 技术文档";
    }

    private String buildContext(List<KnowledgeSearchResultVO> context) {
        if (context == null || context.isEmpty()) {
            return "未命中知识库资料。";
        }
        StringBuilder builder = new StringBuilder();
        int index = 1;
        for (KnowledgeSearchResultVO item : context) {
            builder.append("[").append(index++).append("] ")
                    .append(item.getTitle()).append(" | ")
                    .append(item.getSourcePath()).append("\n")
                    .append(item.getContent()).append("\n\n");
        }
        return builder.toString();
    }

    private String classifyIntent(String message, String mode, String docType) {
        String value = (message == null ? "" : message).toLowerCase(Locale.ROOT);
        if ("document".equalsIgnoreCase(mode) || StringUtils.hasText(docType)) {
            return INTENT_DOCUMENT;
        }
        if (value.contains("导入知识库") || value.contains("重建知识库") || value.contains("索引知识库") || value.contains("更新知识库")) {
            return INTENT_IMPORT;
        }
        if (value.contains("生成") || value.contains("文档") || value.contains("说明书") || value.contains("手册") || value.contains("设计稿")) {
            return INTENT_DOCUMENT;
        }
        return INTENT_QA;
    }

    private AgentSession ensureSession(Integer userId, Integer sessionId, String message, String mode) {
        AgentSession session = sessionId == null ? null : agentSessionMapper.selectById(sessionId);
        if (session != null && !Objects.equals(session.getUserId(), userId)) {
            throw new CustomException(404, "会话不存在");
        }
        if (session == null) {
            String now = nowMillis();
            session = new AgentSession();
            session.setUserId(userId);
            session.setTitle(cleanLine(message, 32));
            session.setMode(mode);
            session.setCreateTime(now);
            session.setUpdateTime(now);
            agentSessionMapper.insert(session);
        }
        return session;
    }

    private void touchSession(AgentSession session) {
        session.setUpdateTime(nowMillis());
        agentSessionMapper.updateById(session);
    }

    private void saveMessage(Integer sessionId, Integer userId, String role, String intent, String content, String citations, Integer resultId) {
        AgentMessage message = new AgentMessage();
        message.setSessionId(sessionId);
        message.setUserId(userId);
        message.setMessageRole(role);
        message.setIntent(intent);
        message.setContent(content);
        message.setCitations(citations);
        message.setResultId(resultId);
        message.setCreateTime(nowMillis());
        agentMessageMapper.insert(message);
    }

    private List<CitationVO> toCitations(List<KnowledgeSearchResultVO> results) {
        List<CitationVO> citations = new ArrayList<>();
        if (results == null) {
            return citations;
        }
        for (KnowledgeSearchResultVO item : results) {
            CitationVO citation = new CitationVO();
            citation.setSourceId(item.getSourceId());
            citation.setChunkId(item.getChunkId());
            citation.setTitle(item.getTitle());
            citation.setSourcePath(item.getSourcePath());
            citation.setSourceType(item.getSourceType());
            citation.setScore(item.getScore());
            citations.add(citation);
        }
        return citations;
    }

    private List<CitationVO> parseCitations(String json) {
        if (!StringUtils.hasText(json)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, CitationVO.class));
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ignored) {
            return "[]";
        }
    }

    private Map<String, String> message(String role, String content) {
        Map<String, String> message = new HashMap<>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private void logTool(Integer userId, Integer taskId, String toolName, String paramsSummary, String status, String errorMessage) {
        try {
            AgentToolLog log = new AgentToolLog();
            log.setUserId(userId);
            log.setTaskId(taskId);
            log.setToolName(toolName);
            log.setParamsSummary(cleanLine(paramsSummary, 500));
            log.setStatus(status);
            log.setErrorMessage(cleanLine(errorMessage, 500));
            log.setCreateTime(nowMillis());
            agentToolLogMapper.insert(log);
        } catch (Exception ignored) {
            // 工具日志不能影响主流程。
        }
    }

    private Integer getCurrentUserId() {
        String token = CommonUtils.getHeader("Authorization");
        if (!StringUtils.hasText(token)) {
            throw new CustomException(401, "请先登录");
        }
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Claims claims = JwtUtils.parseJWT(token);
        Object userObj = claims.get("user");
        if (userObj instanceof Map) {
            Object id = ((Map<?, ?>) userObj).get("id");
            if (id instanceof Integer) {
                return (Integer) id;
            }
            if (id instanceof BigDecimal) {
                return ((BigDecimal) id).intValue();
            }
            if (id instanceof Number) {
                return ((Number) id).intValue();
            }
            if (id != null) {
                return Integer.parseInt(String.valueOf(id));
            }
        }
        throw new CustomException(401, "无法识别当前用户");
    }

    private PageVo defaultPageVo() {
        PageVo pageVo = new PageVo();
        pageVo.setPage(1);
        pageVo.setSize(10);
        return pageVo;
    }

    private String cleanLine(String value, int max) {
        if (value == null) {
            return "";
        }
        String text = value.replaceAll("\\s+", " ").trim();
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, max) + "...";
    }

    private String escapeTable(String value) {
        return value == null ? "" : value.replace("|", "\\|").replace("\n", " ");
    }

    private String nowMillis() {
        return String.valueOf(System.currentTimeMillis());
    }
}
