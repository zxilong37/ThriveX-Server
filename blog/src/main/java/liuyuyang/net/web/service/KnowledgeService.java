package liuyuyang.net.web.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import liuyuyang.net.dto.agent.KnowledgeImportDTO;
import liuyuyang.net.dto.agent.KnowledgeSearchDTO;
import liuyuyang.net.model.KnowledgeSource;
import liuyuyang.net.vo.PageVo;
import liuyuyang.net.vo.agent.KnowledgeSearchResultVO;

import java.util.List;

public interface KnowledgeService {
    List<KnowledgeSource> importDefaultSources();

    KnowledgeSource importSource(KnowledgeImportDTO dto);

    KnowledgeSource reindexSource(Integer id);

    void deleteSource(Integer id);

    Page<KnowledgeSource> listSources(PageVo pageVo);

    List<KnowledgeSearchResultVO> search(KnowledgeSearchDTO dto);
}
