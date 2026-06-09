package liuyuyang.net.dto.hotspot;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import liuyuyang.net.model.Hotspot;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@ApiModel("热点摘要")
public class HotspotSummaryDTO {
    @ApiModelProperty("生成时间戳")
    private String generatedAt;

    @ApiModelProperty("平台数量")
    private Integer totalPlatforms = 0;

    @ApiModelProperty("热点总数")
    private Integer totalItems = 0;

    @ApiModelProperty("最近抓取时间戳")
    private String lastFetchedAt;

    @ApiModelProperty("平台摘要")
    private List<PlatformSummary> platforms = new ArrayList<>();

    @Data
    @ApiModel("平台热点摘要")
    public static class PlatformSummary {
        @ApiModelProperty(value = "平台标识", example = "bilibili")
        private String platform;

        @ApiModelProperty(value = "平台名称", example = "哔哩哔哩")
        private String platformName;

        @ApiModelProperty("热点数量")
        private Integer count = 0;

        @ApiModelProperty("最近抓取时间戳")
        private String latestFetchedAt;

        @ApiModelProperty("排名靠前的热点")
        private List<Hotspot> topItems = new ArrayList<>();
    }
}
