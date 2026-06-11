package liuyuyang.net.model;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@TableName("agent_session")
public class AgentSession extends BaseModel {
    @ApiModelProperty(value = "用户 ID")
    private Integer userId;

    @ApiModelProperty(value = "会话标题")
    private String title;

    @ApiModelProperty(value = "会话模式")
    private String mode;

    @ApiModelProperty(value = "更新时间")
    private String updateTime;
}
