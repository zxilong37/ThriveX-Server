package liuyuyang.net.web.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import liuyuyang.net.common.annotation.NoTokenRequired;
import liuyuyang.net.common.annotation.RateLimit;
import liuyuyang.net.common.utils.Paging;
import liuyuyang.net.common.utils.Result;
import liuyuyang.net.dto.hotspot.HotspotRefreshResultDTO;
import liuyuyang.net.dto.hotspot.HotspotSourceDTO;
import liuyuyang.net.dto.hotspot.HotspotSummaryDTO;
import liuyuyang.net.model.Hotspot;
import liuyuyang.net.vo.hotspot.HotspotFilterVo;
import liuyuyang.net.web.service.HotspotService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Api(tags = "实时热点")
@RestController
@RequestMapping("/hotspot")
@Transactional
public class HotspotController {
    @Resource
    private HotspotService hotspotService;

    @NoTokenRequired
    @RateLimit
    @GetMapping("/sources")
    @ApiOperation("获取热点平台源")
    @ApiOperationSupport(author = "ThriveX", order = 1)
    public Result<List<HotspotSourceDTO>> sources() {
        return Result.success(hotspotService.getSources());
    }

    @NoTokenRequired
    @RateLimit
    @GetMapping
    @ApiOperation(value = "获取实时热点列表", notes = "支持 platform/key/page/size/includeRawJson 查询")
    @ApiOperationSupport(author = "ThriveX", order = 2)
    public Result<Map<String, Object>> list(HotspotFilterVo filterVo) {
        Page<Hotspot> data = hotspotService.getHotspots(filterVo);
        return Result.success(Paging.filter(data));
    }

    @NoTokenRequired
    @RateLimit
    @GetMapping("/summary")
    @ApiOperation("获取实时热点摘要")
    @ApiOperationSupport(author = "ThriveX", order = 3)
    public Result<HotspotSummaryDTO> summary(
            @ApiParam(value = "每个平台返回前N条，默认5") @RequestParam(required = false) Integer top) {
        return Result.success(hotspotService.getSummary(top));
    }

    @PostMapping("/refresh")
    @ApiOperation("手动刷新实时热点")
    @ApiOperationSupport(author = "ThriveX", order = 4)
    public Result<HotspotRefreshResultDTO> refresh() {
        return Result.success(hotspotService.refresh());
    }
}
