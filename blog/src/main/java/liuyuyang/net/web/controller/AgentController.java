package liuyuyang.net.web.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import liuyuyang.net.common.utils.Paging;
import liuyuyang.net.common.utils.Result;
import liuyuyang.net.dto.agent.AgentChatDTO;
import liuyuyang.net.dto.agent.DocumentGenerateDTO;
import liuyuyang.net.dto.agent.DocumentReviewDTO;
import liuyuyang.net.model.AgentSession;
import liuyuyang.net.model.DocumentResult;
import liuyuyang.net.vo.PageVo;
import liuyuyang.net.vo.agent.AgentChatVO;
import liuyuyang.net.vo.agent.DocumentReviewVO;
import liuyuyang.net.web.service.AgentService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/agent")
@Transactional
public class AgentController {
    @Resource
    private AgentService agentService;

    @PostMapping("/chat")
    public Result<AgentChatVO> chat(@RequestBody AgentChatDTO dto) {
        return Result.success(agentService.chat(dto));
    }

    @PostMapping("/document/generate")
    public Result<DocumentResult> generateDocument(@RequestBody DocumentGenerateDTO dto) {
        return Result.success(agentService.generateDocument(dto));
    }

    @PostMapping("/document/review")
    public Result<DocumentReviewVO> reviewDocument(@RequestBody DocumentReviewDTO dto) {
        return Result.success(agentService.reviewDocument(dto));
    }

    @GetMapping("/document/{id}")
    public Result<DocumentResult> getDocument(@PathVariable Integer id) {
        return Result.success(agentService.getDocument(id));
    }

    @PostMapping("/document/list")
    public Result<Map<String, Object>> listDocuments(@RequestBody(required = false) PageVo pageVo) {
        Page<DocumentResult> data = agentService.listDocuments(pageVo);
        return Result.success(Paging.filter(data));
    }

    @GetMapping("/document/{id}/export")
    public ResponseEntity<ByteArrayResource> exportDocument(@PathVariable Integer id) {
        DocumentResult result = agentService.getDocument(id);
        byte[] bytes = result.getContent() == null ? new byte[0] : result.getContent().getBytes(StandardCharsets.UTF_8);
        String fileName = safeFileName(result.getTitle()) + ".md";
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/markdown; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new ByteArrayResource(bytes));
    }

    @PostMapping("/session/list")
    public Result<Map<String, Object>> listSessions(@RequestBody(required = false) PageVo pageVo) {
        Page<AgentSession> data = agentService.listSessions(pageVo);
        return Result.success(Paging.filter(data));
    }

    private String safeFileName(String title) {
        String value = title == null || title.trim().isEmpty() ? "document" : title.trim();
        return value.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
