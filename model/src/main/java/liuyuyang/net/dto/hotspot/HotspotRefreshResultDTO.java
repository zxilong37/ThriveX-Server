package liuyuyang.net.dto.hotspot;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@ApiModel("热点刷新结果")
public class HotspotRefreshResultDTO {
    @ApiModelProperty("刷新开始时间戳")
    private String startedAt;

    @ApiModelProperty("刷新结束时间戳")
    private String finishedAt;

    @ApiModelProperty("源总数")
    private Integer totalSources = 0;

    @ApiModelProperty("成功源数量")
    private Integer successSources = 0;

    @ApiModelProperty("失败源数量")
    private Integer failedSources = 0;

    @ApiModelProperty("抓取条数")
    private Integer fetchedItems = 0;

    @ApiModelProperty("入库条数")
    private Integer savedItems = 0;

    @ApiModelProperty("是否已有任务执行中")
    private Boolean busy = false;

    @ApiModelProperty("各平台刷新结果")
    private List<SourceResult> sources = new ArrayList<>();

    @Data
    @ApiModel("单个平台刷新结果")
    public static class SourceResult {
        @ApiModelProperty(value = "平台标识", example = "weibo")
        private String platform;

        @ApiModelProperty(value = "平台名称", example = "微博")
        private String platformName;

        @ApiModelProperty(value = "是否成功", example = "true")
        private Boolean success = false;

        @ApiModelProperty("抓取条数")
        private Integer fetchedItems = 0;

        @ApiModelProperty("入库条数")
        private Integer savedItems = 0;

        @ApiModelProperty("失败原因")
        private String message;
    }
}
