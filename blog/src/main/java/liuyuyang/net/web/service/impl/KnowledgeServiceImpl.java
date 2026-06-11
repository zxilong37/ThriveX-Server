package liuyuyang.net.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import liuyuyang.net.common.execption.CustomException;
import liuyuyang.net.common.utils.CommonUtils;
import liuyuyang.net.dto.agent.KnowledgeImportDTO;
import liuyuyang.net.dto.agent.KnowledgeSearchDTO;
import liuyuyang.net.model.KnowledgeChunk;
import liuyuyang.net.model.KnowledgeSource;
import liuyuyang.net.vo.PageVo;
import liuyuyang.net.vo.agent.KnowledgeSearchResultVO;
import liuyuyang.net.web.mapper.KnowledgeChunkMapper;
import liuyuyang.net.web.mapper.KnowledgeSourceMapper;
import liuyuyang.net.web.service.KnowledgeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
public class KnowledgeServiceImpl implements KnowledgeService {
    private static final int DEFAULT_SEARCH_LIMIT = 8;
    private static final int MAX_CHUNK_LENGTH = 4200;
    private static final Pattern MARKDOWN_HEADING = Pattern.compile("^(#{1,6})\\s+(.+)$");

    @Resource
    private KnowledgeSourceMapper knowledgeSourceMapper;
    @Resource
    private KnowledgeChunkMapper knowledgeChunkMapper;
    @Resource
    private CommonUtils commonUtils;

    @Override
    public List<KnowledgeSource> importDefaultSources() {
        List<DefaultSource> sources = discoverDefaultSources();
        List<KnowledgeSource> result = new ArrayList<>();
        for (DefaultSource source : sources) {
            if (!Files.exists(source.path) || !Files.isRegularFile(source.path) || isSensitivePath(source.path)) {
                continue;
            }
            try {
                KnowledgeImportDTO dto = new KnowledgeImportDTO();
                dto.setName(source.name);
                dto.setSourceType(source.sourceType);
                dto.setSourcePath(toWorkspacePath(source.path));
                dto.setDescription(source.description);
                dto.setContent(new String(Files.readAllBytes(source.path), StandardCharsets.UTF_8));
                result.add(importSource(dto));
            } catch (IOException ex) {
                throw new CustomException(500, "导入知识库失败：" + source.path + "，" + ex.getMessage());
            }
        }
        return result;
    }

    @Override
    public KnowledgeSource importSource(KnowledgeImportDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getContent())) {
            throw new CustomException(400, "知识内容不能为空");
        }
        if (isSensitivePath(dto.getSourcePath())) {
            throw new CustomException(400, "敏感配置文件不能导入知识库");
        }

        String sourcePath = StringUtils.hasText(dto.getSourcePath()) ? dto.getSourcePath().trim() : "manual://" + System.currentTimeMillis();
        KnowledgeSource source = findSource(sourcePath);
        String now = nowMillis();
        if (source == null) {
            source = new KnowledgeSource();
            source.setCreateTime(now);
            source.setEnabled(1);
        }
        source.setName(StringUtils.hasText(dto.getName()) ? dto.getName().trim() : sourcePath);
        source.setSourceType(StringUtils.hasText(dto.getSourceType()) ? dto.getSourceType().trim() : detectSourceType(sourcePath));
        source.setSourcePath(sourcePath);
        source.setDescription(dto.getDescription());
        source.setIndexStatus("indexing");
        source.setUpdateTime(now);
        if (source.getId() == null) {
            knowledgeSourceMapper.insert(source);
        } else {
            knowledgeSourceMapper.updateById(source);
            deleteChunks(source.getId());
        }

        List<ChunkDraft> drafts = splitContent(source, dto.getContent());
        int order = 1;
        for (ChunkDraft draft : drafts) {
            KnowledgeChunk chunk = new KnowledgeChunk();
            chunk.setSourceId(source.getId());
            chunk.setTitle(limit(draft.title, 255));
            chunk.setContent(draft.content);
            chunk.setSourcePath(source.getSourcePath());
            chunk.setSourceType(source.getSourceType());
            chunk.setTags(source.getSourceType());
            chunk.setChunkOrder(order++);
            chunk.setContentHash(DigestUtils.md5DigestAsHex(draft.content.getBytes(StandardCharsets.UTF_8)));
            chunk.setCreateTime(now);
            knowledgeChunkMapper.insert(chunk);
        }

        source.setChunkCount(drafts.size());
        source.setIndexStatus("ready");
        source.setLastIndexedTime(now);
        source.setUpdateTime(now);
        knowledgeSourceMapper.updateById(source);
        return source;
    }

    @Override
    public KnowledgeSource reindexSource(Integer id) {
        KnowledgeSource source = knowledgeSourceMapper.selectById(id);
        if (source == null) {
            throw new CustomException(404, "知识来源不存在");
        }
        if (isSensitivePath(source.getSourcePath())) {
            throw new CustomException(400, "敏感配置文件不能导入知识库");
        }

        Path path = resolveWorkspacePath(source.getSourcePath());
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new CustomException(404, "知识来源文件不存在");
        }
        try {
            KnowledgeImportDTO dto = new KnowledgeImportDTO();
            dto.setName(source.getName());
            dto.setSourceType(source.getSourceType());
            dto.setSourcePath(source.getSourcePath());
            dto.setDescription(source.getDescription());
            dto.setContent(new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
            return importSource(dto);
        } catch (IOException ex) {
            throw new CustomException(500, "重建索引失败：" + ex.getMessage());
        }
    }

    @Override
    public void deleteSource(Integer id) {
        KnowledgeSource source = knowledgeSourceMapper.selectById(id);
        if (source == null) {
            throw new CustomException(404, "知识来源不存在");
        }
        deleteChunks(id);
        knowledgeSourceMapper.deleteById(id);
    }

    @Override
    public Page<KnowledgeSource> listSources(PageVo pageVo) {
        LambdaQueryWrapper<KnowledgeSource> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(KnowledgeSource::getUpdateTime).orderByDesc(KnowledgeSource::getCreateTime);
        List<KnowledgeSource> list = knowledgeSourceMapper.selectList(wrapper);
        return commonUtils.getPageData(pageVo == null ? defaultPageVo() : pageVo, list);
    }

    @Override
    public List<KnowledgeSearchResultVO> search(KnowledgeSearchDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getQuery())) {
            return Collections.emptyList();
        }
        int limit = dto.getLimit() == null ? DEFAULT_SEARCH_LIMIT : Math.min(Math.max(dto.getLimit(), 1), 20);
        List<String> terms = buildTerms(dto.getQuery());

        LambdaQueryWrapper<KnowledgeChunk> wrapper = new LambdaQueryWrapper<>();
        wrapper.inSql(KnowledgeChunk::getSourceId, "select id from knowledge_source where enabled = 1");
        List<KnowledgeChunk> chunks = knowledgeChunkMapper.selectList(wrapper);

        List<KnowledgeSearchResultVO> results = new ArrayList<>();
        String normalizedQuery = normalize(dto.getQuery());
        for (KnowledgeChunk chunk : chunks) {
            double score = score(chunk, terms, normalizedQuery);
            if (score <= 0) {
                continue;
            }
            KnowledgeSearchResultVO vo = new KnowledgeSearchResultVO();
            vo.setSourceId(chunk.getSourceId());
            vo.setChunkId(chunk.getId());
            vo.setTitle(chunk.getTitle());
            vo.setSourcePath(chunk.getSourcePath());
            vo.setSourceType(chunk.getSourceType());
            vo.setContent(snippet(chunk.getContent(), terms, 1200));
            vo.setScore(score);
            results.add(vo);
        }

        results.sort(new Comparator<KnowledgeSearchResultVO>() {
            @Override
            public int compare(KnowledgeSearchResultVO o1, KnowledgeSearchResultVO o2) {
                return Double.compare(o2.getScore(), o1.getScore());
            }
        });
        return results.size() > limit ? new ArrayList<>(results.subList(0, limit)) : results;
    }

    private List<DefaultSource> discoverDefaultSources() {
        Path root = workspaceRoot();
        List<DefaultSource> sources = new ArrayList<>();
        addSource(sources, root.resolve("AGENTS.md"), "AGENTS.md", "workspace", "工作区多 Agent 开发说明");
        addSource(sources, root.resolve("ThriveX-Server").resolve("README.md"), "Server README", "readme", "后端项目说明");
        addSource(sources, root.resolve("ThriveX-Admin").resolve("README.md"), "Admin README", "readme", "后台项目说明");
        addSource(sources, root.resolve("ThriveX-Blog").resolve("README.md"), "Blog README", "readme", "博客前台项目说明");
        addSource(sources, root.resolve("ThriveX-Server").resolve("ThriveX.sql"), "ThriveX.sql", "sql", "数据库总 SQL");

        Path docsDir = root.resolve("ThriveX-Blog").resolve("docs");
        if (Files.exists(docsDir) && Files.isDirectory(docsDir)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(docsDir, "*.md")) {
                for (Path path : stream) {
                    addSource(sources, path, "Blog docs/" + path.getFileName().toString(), "markdown", "项目文档");
                }
            } catch (IOException ignored) {
                // 默认导入不因 docs 目录读取失败而中断。
            }
        }
        return sources;
    }

    private void addSource(List<DefaultSource> sources, Path path, String name, String type, String description) {
        sources.add(new DefaultSource(path, name, type, description));
    }

    private List<ChunkDraft> splitContent(KnowledgeSource source, String content) {
        List<ChunkDraft> chunks;
        if ("sql".equalsIgnoreCase(source.getSourceType()) || source.getSourcePath().toLowerCase(Locale.ROOT).endsWith(".sql")) {
            chunks = splitSql(content);
        } else {
            chunks = splitMarkdown(source.getName(), content);
        }
        if (chunks.isEmpty()) {
            chunks.add(new ChunkDraft(source.getName(), content));
        }
        return chunks;
    }

    private List<ChunkDraft> splitMarkdown(String defaultTitle, String content) {
        List<ChunkDraft> chunks = new ArrayList<>();
        String[] lines = content.replace("\r\n", "\n").split("\n");
        String title = defaultTitle;
        StringBuilder current = new StringBuilder();
        for (String line : lines) {
            Matcher matcher = MARKDOWN_HEADING.matcher(line);
            boolean heading = matcher.find();
            if (heading && current.length() > 0) {
                addSizedChunk(chunks, title, current.toString());
                current.setLength(0);
                title = matcher.group(2).trim();
            }
            if (heading) {
                title = matcher.group(2).trim();
            }
            current.append(line).append('\n');
        }
        if (current.length() > 0) {
            addSizedChunk(chunks, title, current.toString());
        }
        return chunks;
    }

    private List<ChunkDraft> splitSql(String content) {
        List<ChunkDraft> chunks = new ArrayList<>();
        String[] sections = content.split("(?=--\\s*Table structure for table)");
        for (String section : sections) {
            if (!StringUtils.hasText(section)) {
                continue;
            }
            String title = "SQL";
            Matcher matcher = Pattern.compile("`([^`]+)`").matcher(section);
            if (matcher.find()) {
                title = "SQL table: " + matcher.group(1);
            }
            addSizedChunk(chunks, title, section);
        }
        return chunks;
    }

    private void addSizedChunk(List<ChunkDraft> chunks, String title, String content) {
        if (!StringUtils.hasText(content)) {
            return;
        }
        String trimmed = content.trim();
        if (trimmed.length() <= MAX_CHUNK_LENGTH) {
            chunks.add(new ChunkDraft(title, trimmed));
            return;
        }
        int index = 1;
        int start = 0;
        while (start < trimmed.length()) {
            int end = Math.min(start + MAX_CHUNK_LENGTH, trimmed.length());
            chunks.add(new ChunkDraft(title + " #" + index, trimmed.substring(start, end)));
            start = end;
            index++;
        }
    }

    private double score(KnowledgeChunk chunk, List<String> terms, String normalizedQuery) {
        String title = normalize(chunk.getTitle());
        String content = normalize(chunk.getContent());
        String sourcePath = normalize(chunk.getSourcePath());
        double score = 0;
        if (StringUtils.hasText(normalizedQuery)) {
            if (title.contains(normalizedQuery)) {
                score += 30;
            }
            if (content.contains(normalizedQuery)) {
                score += 18;
            }
            if (sourcePath.contains(normalizedQuery)) {
                score += 12;
            }
        }
        for (String term : terms) {
            if (term.length() < 2) {
                continue;
            }
            if (title.contains(term)) {
                score += 10;
            }
            if (sourcePath.contains(term)) {
                score += 6;
            }
            int contentHits = countHits(content, term);
            if (contentHits > 0) {
                score += Math.min(contentHits, 6) * 2;
            }
        }
        return score;
    }

    private List<String> buildTerms(String query) {
        Set<String> terms = new LinkedHashSet<>();
        String normalized = normalize(query);
        for (String term : normalized.split("\\s+")) {
            if (StringUtils.hasText(term)) {
                terms.add(term);
                if (containsChinese(term) && term.length() > 2) {
                    for (int i = 0; i < term.length() - 1; i++) {
                        terms.add(term.substring(i, i + 2));
                    }
                }
            }
        }
        return new ArrayList<>(terms);
    }

    private boolean containsChinese(String value) {
        for (int i = 0; i < value.length(); i++) {
            Character.UnicodeBlock block = Character.UnicodeBlock.of(value.charAt(i));
            if (block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                    || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                    || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A) {
                return true;
            }
        }
        return false;
    }

    private int countHits(String text, String term) {
        int count = 0;
        int from = 0;
        while (from >= 0 && from < text.length()) {
            int index = text.indexOf(term, from);
            if (index < 0) {
                break;
            }
            count++;
            from = index + term.length();
        }
        return count;
    }

    private String snippet(String content, List<String> terms, int limit) {
        if (content == null) {
            return "";
        }
        int start = 0;
        String normalized = normalize(content);
        for (String term : terms) {
            int index = normalized.indexOf(term);
            if (index >= 0) {
                start = Math.max(0, index - 120);
                break;
            }
        }
        int end = Math.min(content.length(), start + limit);
        return content.substring(start, end);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[`*_#>\\[\\]{}()<>:;,.!?，。！？；：、/\\\\|\\-]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private KnowledgeSource findSource(String sourcePath) {
        LambdaQueryWrapper<KnowledgeSource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeSource::getSourcePath, sourcePath);
        return knowledgeSourceMapper.selectOne(wrapper);
    }

    private void deleteChunks(Integer sourceId) {
        LambdaQueryWrapper<KnowledgeChunk> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeChunk::getSourceId, sourceId);
        knowledgeChunkMapper.delete(wrapper);
    }

    private String detectSourceType(String sourcePath) {
        String lower = sourcePath == null ? "" : sourcePath.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".sql")) {
            return "sql";
        }
        if (lower.endsWith(".md") || lower.endsWith(".markdown")) {
            return "markdown";
        }
        if (lower.endsWith(".txt")) {
            return "text";
        }
        return "manual";
    }

    private boolean isSensitivePath(Path path) {
        return path != null && isSensitivePath(path.toString());
    }

    private boolean isSensitivePath(String path) {
        if (!StringUtils.hasText(path)) {
            return false;
        }
        String lower = path.toLowerCase(Locale.ROOT);
        return lower.contains(".env")
                || lower.contains("application-pro.yml")
                || lower.contains("application-prod.yml")
                || lower.contains("application-production.yml")
                || lower.contains("node_modules")
                || lower.contains("target")
                || lower.contains("dist");
    }

    private Path resolveWorkspacePath(String sourcePath) {
        Path path = Paths.get(sourcePath);
        if (path.isAbsolute()) {
            return path;
        }
        return workspaceRoot().resolve(sourcePath);
    }

    private String toWorkspacePath(Path path) {
        Path root = workspaceRoot();
        try {
            return root.relativize(path.toAbsolutePath().normalize()).toString().replace('\\', '/');
        } catch (Exception ignored) {
            return path.toString().replace('\\', '/');
        }
    }

    private Path workspaceRoot() {
        Path current = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        if ("ThriveX-Server".equalsIgnoreCase(current.getFileName().toString())) {
            return current.getParent();
        }
        if (Files.exists(current.resolve("ThriveX-Server"))) {
            return current;
        }
        return current.getParent() == null ? current : current.getParent();
    }

    private PageVo defaultPageVo() {
        PageVo pageVo = new PageVo();
        pageVo.setPage(1);
        pageVo.setSize(10);
        return pageVo;
    }

    private String limit(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    private String nowMillis() {
        return String.valueOf(System.currentTimeMillis());
    }

    private static class DefaultSource {
        private final Path path;
        private final String name;
        private final String sourceType;
        private final String description;

        private DefaultSource(Path path, String name, String sourceType, String description) {
            this.path = path;
            this.name = name;
            this.sourceType = sourceType;
            this.description = description;
        }
    }

    private static class ChunkDraft {
        private final String title;
        private final String content;

        private ChunkDraft(String title, String content) {
            this.title = title;
            this.content = content;
        }
    }
}
