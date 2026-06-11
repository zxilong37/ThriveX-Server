package liuyuyang.net.vo.agent;

import lombok.Data;

@Data
public class KnowledgeSearchResultVO {
    private Integer sourceId;
    private Integer chunkId;
    private String title;
    private String sourcePath;
    private String sourceType;
    private String content;
    private Double score;
}
