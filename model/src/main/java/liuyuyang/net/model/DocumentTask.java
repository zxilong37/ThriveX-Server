package liuyuyang.net.model;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@TableName("document_task")
public class DocumentTask extends BaseModel {
    @ApiModelProperty(value = "用户 ID")
    private Integer userId;

    @ApiModelProperty(value = "会话 ID")
    private Integer sessionId;

    @ApiModelProperty(value = "文档标题")
    private String title;

    @ApiModelProperty(value = "文档类型")
    private String docType;

    @ApiModelProperty(value = "任务状态")
    private String status;

    @ApiModelProperty(value = "用户提示词")
    private String prompt;

    @ApiModelProperty(value = "文档大纲")
    private String outline;

    @ApiModelProperty(value = "引用来源 JSON")
    private String citations;

    @ApiModelProperty(value = "更新时间")
    private String updateTime;
}
