package liuyuyang.net.dto.hotspot;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Map;

@Data
@ApiModel("热点源配置")
public class HotspotSourceDTO {
    @ApiModelProperty(value = "平台标识", example = "zhihu")
    private String platform;

    @ApiModelProperty(value = "平台名称", example = "知乎")
    private String platformName;

    @ApiModelProperty(value = "抓取地址")
    private String url;

    @ApiModelProperty(value = "请求头配置，用于第三方 API Token")
    private Map<String, String> headers;

    @ApiModelProperty(value = "是否启用", example = "true")
    private Boolean enabled;
}
