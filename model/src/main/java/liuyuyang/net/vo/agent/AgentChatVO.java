package liuyuyang.net.vo.agent;

import lombok.Data;

import java.util.List;

@Data
public class AgentChatVO {
    private Integer sessionId;
    private String intent;
    private String answer;
    private Integer resultId;
    private List<CitationVO> citations;
}
