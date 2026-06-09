package liuyuyang.net.web.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import liuyuyang.net.dto.hotspot.HotspotRefreshResultDTO;
import liuyuyang.net.dto.hotspot.HotspotSourceDTO;
import liuyuyang.net.dto.hotspot.HotspotSummaryDTO;
import liuyuyang.net.model.Hotspot;
import liuyuyang.net.vo.hotspot.HotspotFilterVo;

import java.util.List;

public interface HotspotService extends IService<Hotspot> {
    List<HotspotSourceDTO> getSources();

    Page<Hotspot> getHotspots(HotspotFilterVo filterVo);

    HotspotSummaryDTO getSummary(Integer top);

    HotspotRefreshResultDTO refresh();
}
