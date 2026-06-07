package liuyuyang.net.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@TableName("user")
public class User extends BaseModel {
    @ApiModelProperty(value = "用户账号", example = "liuyuyang", required = true)
    private String username;

    @ApiModelProperty(value = "用户密码", required = true)
    private String password;

    @ApiModelProperty(value = "用户名称", example = "郑州 GIS 开发工程师", required = true)
    private String name;

    @ApiModelProperty(value = "用户介绍", example = "再渺小的星光，也有属于他的光芒!")
    private String info;

    @ApiModelProperty(value = "用户邮箱", example = "2069065992@qq.com")
    private String email;

    @ApiModelProperty(value = "用户头像", example = "yuyang.jpg")
    private String avatar;
}
