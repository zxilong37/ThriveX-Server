package liuyuyang.net.web.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import liuyuyang.net.common.annotation.NoTokenRequired;
import liuyuyang.net.common.annotation.RateLimit;
import liuyuyang.net.model.Record;
import liuyuyang.net.common.utils.Result;
import liuyuyang.net.web.service.RecordService;
import liuyuyang.net.common.utils.Paging;
import liuyuyang.net.vo.FilterVo;
import liuyuyang.net.vo.PageVo;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Api(tags = "说说管理")
@RestController
@RequestMapping("/record")
@Transactional
public class RecordController {
    @Resource
    private RecordService recordService;

    @PostMapping
    @ApiOperation("新增说说")
    @ApiOperationSupport(author = "郑州 GIS 开发工程师 | 2069065992@qq.com", order = 1)
    public Result<String> add(@RequestBody Record record) {
        recordService.save(record);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除说说")
    @ApiOperationSupport(author = "郑州 GIS 开发工程师 | 2069065992@qq.com", order = 2)
    public Result<String> del(@PathVariable Integer id) {
        recordService.removeById(id);
        return Result.success();
    }

    @PatchMapping
    @ApiOperation("编辑说说")
    @ApiOperationSupport(author = "郑州 GIS 开发工程师 | 2069065992@qq.com", order = 4)
    public Result<String> edit(@RequestBody Record record) {
        recordService.updateById(record);
        return Result.success();
    }

    @RateLimit
    @GetMapping("/{id}")
    @ApiOperation("获取说说")
    @ApiOperationSupport(author = "郑州 GIS 开发工程师 | 2069065992@qq.com", order = 5)
    public Result<Record> get(@PathVariable Integer id) {
        Record data = recordService.getById(id);
        return Result.success(data);
    }

    @RateLimit
    @NoTokenRequired
    @PostMapping("/list")
    @ApiOperation("获取说说列表")
    @ApiOperationSupport(author = "郑州 GIS 开发工程师 | 2069065992@qq.com", order = 6)
    public Result<List<Record>> list(@RequestBody FilterVo filterVo) {
        List<Record> data = recordService.list(filterVo);
        return Result.success(data);
    }

    @RateLimit
    @NoTokenRequired
    @PostMapping("/paging")
    @ApiOperation("分页查询说说列表")
    @ApiOperationSupport(author = "郑州 GIS 开发工程师 | 2069065992@qq.com", order = 7)
    public Result paging(@RequestBody FilterVo filterVo, PageVo pageVo) {
        Page<Record> data = recordService.paging(filterVo, pageVo);
        Map<String, Object> result = Paging.filter(data);
        return Result.success(result);
    }
}
