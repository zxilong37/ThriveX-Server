package liuyuyang.net.vo.agent;

import lombok.Data;

import java.util.List;

@Data
public class DocumentReviewVO {
    private Integer score;
    private Boolean passed;
    private List<String> issues;
}
