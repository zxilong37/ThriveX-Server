package liuyuyang.net.vo.hotspot;

import io.swagger.annotations.ApiParam;
import liuyuyang.net.vo.PageVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class HotspotFilterVo extends PageVo {
    @ApiParam(value = "平台标识，不传则返回全部")
    private String platform;

    @ApiParam(value = "标题或摘要关键词")
    private String key;

    @ApiParam(value = "是否返回原始JSON，默认false")
    private Boolean includeRawJson = false;
}
