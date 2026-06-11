package liuyuyang.net.model;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@TableName("document_result")
public class DocumentResult extends BaseModel {
    @ApiModelProperty(value = "任务 ID")
    private Integer taskId;

    @ApiModelProperty(value = "用户 ID")
    private Integer userId;

    @ApiModelProperty(value = "文档标题")
    private String title;

    @ApiModelProperty(value = "文档类型")
    private String docType;

    @ApiModelProperty(value = "输出格式")
    private String format;

    @ApiModelProperty(value = "文档正文")
    private String content;

    @ApiModelProperty(value = "引用来源 JSON")
    private String citations;

    @ApiModelProperty(value = "审校分数")
    private Integer reviewScore;

    @ApiModelProperty(value = "审校是否通过")
    private Integer reviewPassed;

    @ApiModelProperty(value = "审校问题 JSON")
    private String reviewIssues;

    @ApiModelProperty(value = "更新时间")
    private String updateTime;
}
