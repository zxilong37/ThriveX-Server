package liuyuyang.net.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("work_report")
public class WorkReport extends BaseModel {
    private Integer userId;
    private String type;
    private String period;
    private String title;
    private String summary;
    private String details;
    private String nextPlan;
    private String attachmentNote;
    private String status;
    private Integer draftVersion;
    private String updateTime;
}
