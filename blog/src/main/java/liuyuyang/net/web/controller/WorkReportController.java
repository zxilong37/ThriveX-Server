package liuyuyang.net.web.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import liuyuyang.net.common.execption.CustomException;
import liuyuyang.net.common.utils.Paging;
import liuyuyang.net.common.utils.Result;
import liuyuyang.net.dto.workreport.WorkReportDTO;
import liuyuyang.net.dto.workreport.WorkReportScheduleDTO;
import liuyuyang.net.model.WorkReport;
import liuyuyang.net.model.WorkReportExport;
import liuyuyang.net.model.WorkReportSchedule;
import liuyuyang.net.vo.PageVo;
import liuyuyang.net.web.service.WorkReportService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/work_report")
@Transactional
public class WorkReportController {
    @Resource
    private WorkReportService workReportService;

    @GetMapping
    public Result<WorkReport> get(@RequestParam String type, @RequestParam String period) {
        return Result.success(workReportService.getCurrentUserReport(type, period));
    }

    @PostMapping
    public Result<WorkReport> save(@RequestBody WorkReportDTO dto) {
        return Result.success(workReportService.saveCurrentUserReport(dto));
    }

    @PatchMapping("/{id}")
    public Result<WorkReport> update(@PathVariable Integer id, @RequestBody WorkReportDTO dto) {
        return Result.success(workReportService.updateCurrentUserReport(id, dto));
    }

    @GetMapping("/list")
    public Result<Map<String, Object>> list(@RequestParam(required = false) String type, PageVo pageVo) {
        Page<WorkReport> data = workReportService.listCurrentUserReports(type, pageVo);
        return Result.success(Paging.filter(data));
    }

    @PostMapping("/{id}/export")
    public Result<WorkReportExport> export(@PathVariable Integer id, @RequestParam(required = false, defaultValue = "manual") String source) {
        return Result.success(workReportService.exportCurrentUserReport(id, source));
    }

    @GetMapping("/export/list")
    public Result<Map<String, Object>> exportList(PageVo pageVo) {
        Page<WorkReportExport> data = workReportService.listCurrentUserExports(pageVo);
        return Result.success(Paging.filter(data));
    }

    @GetMapping("/export/{id}/download")
    public ResponseEntity<org.springframework.core.io.Resource> download(@PathVariable Integer id) {
        WorkReportExport export = workReportService.getCurrentUserExport(id);
        File file = new File(export.getFilePath());
        if (!file.exists()) {
            throw new CustomException(404, "导出文件不存在");
        }
        FileSystemResource resource = new FileSystemResource(file);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(export.getFileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(resource);
    }

    @GetMapping("/schedule")
    public Result<List<WorkReportSchedule>> schedule() {
        return Result.success(workReportService.getCurrentUserSchedules());
    }

    @PatchMapping("/schedule")
    public Result<List<WorkReportSchedule>> updateSchedule(@RequestBody List<WorkReportScheduleDTO> schedules) {
        return Result.success(workReportService.updateCurrentUserSchedules(schedules));
    }
}
