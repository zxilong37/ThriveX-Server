package liuyuyang.net.dto.agent;

import lombok.Data;

@Data
public class DocumentGenerateDTO {
    private Integer sessionId;
    private String prompt;
    private String title;
    private String docType;
    private String outputFormat;
}
