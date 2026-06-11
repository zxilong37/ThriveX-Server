package liuyuyang.net.model;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@TableName("agent_tool_log")
public class AgentToolLog extends BaseModel {
    @ApiModelProperty(value = "用户 ID")
    private Integer userId;

    @ApiModelProperty(value = "任务 ID")
    private Integer taskId;

    @ApiModelProperty(value = "工具名称")
    private String toolName;

    @ApiModelProperty(value = "参数摘要")
    private String paramsSummary;

    @ApiModelProperty(value = "执行状态")
    private String status;

    @ApiModelProperty(value = "错误信息")
    private String errorMessage;
}
