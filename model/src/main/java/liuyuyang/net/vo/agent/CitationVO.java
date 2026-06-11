package liuyuyang.net.vo.agent;

import lombok.Data;

@Data
public class CitationVO {
    private Integer sourceId;
    private Integer chunkId;
    private String title;
    private String sourcePath;
    private String sourceType;
    private Double score;
}
