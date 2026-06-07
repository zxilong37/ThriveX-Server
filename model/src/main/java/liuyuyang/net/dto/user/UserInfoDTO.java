package liuyuyang.net.dto.user;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class UserInfoDTO {
    @ApiModelProperty(value = "用户ID")
    private Integer id;

    @ApiModelProperty(value = "用户账号", example = "liuyuyang", required = true)
    private String username;

    @ApiModelProperty(value = "用户名称", example = "郑州 GIS 开发工程师", required = true)
    private String name;

    @ApiModelProperty(value = "用户介绍", example = "再渺小的星光，也有属于他的光芒!")
    private String info;

    @ApiModelProperty(value = "用户邮箱", example = "2069065992@qq.com")
    private String email;

    @ApiModelProperty(value = "用户头像", example = "yuyang.jpg")
    private String avatar;
}