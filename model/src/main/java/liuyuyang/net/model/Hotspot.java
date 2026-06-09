package liuyuyang.net.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("实时热点")
@TableName("hotspot")
public class Hotspot {
    @TableId(type = IdType.AUTO)
    @ApiModelProperty("热点ID")
    private Integer id;

    @ApiModelProperty(value = "平台标识", example = "weibo")
    private String platform;

    @ApiModelProperty(value = "平台名称", example = "微博")
    private String platformName;

    @ApiModelProperty(value = "热点标题", example = "今日热点标题")
    private String title;

    @ApiModelProperty(value = "热点链接")
    private String url;

    @ApiModelProperty(value = "封面图")
    private String cover;

    @ApiModelProperty(value = "摘要")
    private String summary;

    @ApiModelProperty(value = "榜单排名", example = "1")
    private Integer rankNo;

    @ApiModelProperty(value = "热度值", example = "123456")
    private String hotValue;

    @ApiModelProperty(value = "抓取时间戳", example = "1723533206613")
    private String fetchedAt;

    @ApiModelProperty(value = "原始JSON")
    private String rawJson;

    @TableField(select = false)
    private String titleHash;

    @TableField(select = false)
    private String linkHash;

    @ApiModelProperty(value = "创建时间戳", example = "1723533206613")
    private String createdAt;

    @ApiModelProperty(value = "更新时间戳", example = "1723533206613")
    private String updatedAt;
}
