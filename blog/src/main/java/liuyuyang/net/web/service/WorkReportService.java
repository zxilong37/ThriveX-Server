package liuyuyang.net.web.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import liuyuyang.net.dto.workreport.WorkReportDTO;
import liuyuyang.net.dto.workreport.WorkReportScheduleDTO;
import liuyuyang.net.model.WorkReport;
import liuyuyang.net.model.WorkReportExport;
import liuyuyang.net.model.WorkReportSchedule;
import liuyuyang.net.vo.PageVo;

import java.util.List;

public interface WorkReportService extends IService<WorkReport> {
    WorkReport getCurrentUserReport(String type, String period);

    WorkReport saveCurrentUserReport(WorkReportDTO dto);

    WorkReport updateCurrentUserReport(Integer id, WorkReportDTO dto);

    Page<WorkReport> listCurrentUserReports(String type, PageVo pageVo);

    WorkReportExport exportCurrentUserReport(Integer id, String source);

    Page<WorkReportExport> listCurrentUserExports(PageVo pageVo);

    List<WorkReportSchedule> getCurrentUserSchedules();

    List<WorkReportSchedule> updateCurrentUserSchedules(List<WorkReportScheduleDTO> schedules);

    WorkReportExport getCurrentUserExport(Integer id);

    void scanScheduledExports();
}
