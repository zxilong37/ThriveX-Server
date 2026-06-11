package liuyuyang.net.dto.agent;

import lombok.Data;

@Data
public class AgentChatDTO {
    private Integer sessionId;
    private String message;
    private String mode;
    private String docType;
    private String outputFormat;
}
