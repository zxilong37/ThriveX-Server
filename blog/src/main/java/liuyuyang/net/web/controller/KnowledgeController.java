package liuyuyang.net.web.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import liuyuyang.net.common.utils.Paging;
import liuyuyang.net.common.utils.Result;
import liuyuyang.net.dto.agent.KnowledgeImportDTO;
import liuyuyang.net.dto.agent.KnowledgeSearchDTO;
import liuyuyang.net.model.KnowledgeSource;
import liuyuyang.net.vo.PageVo;
import liuyuyang.net.vo.agent.KnowledgeSearchResultVO;
import liuyuyang.net.web.service.KnowledgeService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/knowledge")
@Transactional
public class KnowledgeController {
    @Resource
    private KnowledgeService knowledgeService;

    @PostMapping("/source/import-default")
    public Result<List<KnowledgeSource>> importDefaultSources() {
        return Result.success(knowledgeService.importDefaultSources());
    }

    @PostMapping("/source")
    public Result<KnowledgeSource> importSource(@RequestBody KnowledgeImportDTO dto) {
        return Result.success(knowledgeService.importSource(dto));
    }

    @PostMapping("/source/list")
    public Result<Map<String, Object>> listSources(@RequestBody(required = false) PageVo pageVo) {
        Page<KnowledgeSource> data = knowledgeService.listSources(pageVo);
        return Result.success(Paging.filter(data));
    }

    @PostMapping("/source/{id}/index")
    public Result<KnowledgeSource> reindexSource(@PathVariable Integer id) {
        return Result.success(knowledgeService.reindexSource(id));
    }

    @DeleteMapping("/source/{id}")
    public Result<String> deleteSource(@PathVariable Integer id) {
        knowledgeService.deleteSource(id);
        return Result.success();
    }

    @PostMapping("/search")
    public Result<List<KnowledgeSearchResultVO>> search(@RequestBody KnowledgeSearchDTO dto) {
        return Result.success(knowledgeService.search(dto));
    }
}
