package liuyuyang.net.model;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@TableName("agent_message")
public class AgentMessage extends BaseModel {
    @ApiModelProperty(value = "会话 ID")
    private Integer sessionId;

    @ApiModelProperty(value = "用户 ID")
    private Integer userId;

    @ApiModelProperty(value = "消息角色")
    private String messageRole;

    @ApiModelProperty(value = "识别意图")
    private String intent;

    @ApiModelProperty(value = "消息内容")
    private String content;

    @ApiModelProperty(value = "引用来源 JSON")
    private String citations;

    @ApiModelProperty(value = "关联结果 ID")
    private Integer resultId;
}
