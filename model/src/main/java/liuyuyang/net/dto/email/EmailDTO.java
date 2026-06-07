package liuyuyang.net.dto.email;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class EmailDTO {
    @ApiModelProperty(value = "邮件接收者", example = "2069065992@qq.com")
    private String to;
    @ApiModelProperty(value = "邮件标题", example = "这是一段标题", required = true)
    private String subject;
}
