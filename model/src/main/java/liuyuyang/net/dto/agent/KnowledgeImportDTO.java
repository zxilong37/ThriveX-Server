package liuyuyang.net.dto.agent;

import lombok.Data;

@Data
public class KnowledgeImportDTO {
    private String name;
    private String sourceType;
    private String sourcePath;
    private String description;
    private String content;
}
