package liuyuyang.net.web.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import liuyuyang.net.dto.agent.AgentChatDTO;
import liuyuyang.net.dto.agent.DocumentGenerateDTO;
import liuyuyang.net.dto.agent.DocumentReviewDTO;
import liuyuyang.net.model.AgentSession;
import liuyuyang.net.model.DocumentResult;
import liuyuyang.net.vo.PageVo;
import liuyuyang.net.vo.agent.AgentChatVO;
import liuyuyang.net.vo.agent.DocumentReviewVO;

public interface AgentService {
    AgentChatVO chat(AgentChatDTO dto);

    DocumentResult generateDocument(DocumentGenerateDTO dto);

    DocumentReviewVO reviewDocument(DocumentReviewDTO dto);

    DocumentResult getDocument(Integer id);

    Page<DocumentResult> listDocuments(PageVo pageVo);

    Page<AgentSession> listSessions(PageVo pageVo);
}
