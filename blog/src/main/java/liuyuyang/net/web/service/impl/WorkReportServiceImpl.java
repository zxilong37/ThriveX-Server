package liuyuyang.net.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.jsonwebtoken.Claims;
import liuyuyang.net.common.execption.CustomException;
import liuyuyang.net.common.utils.CommonUtils;
import liuyuyang.net.common.utils.JwtUtils;
import liuyuyang.net.dto.workreport.WorkReportDTO;
import liuyuyang.net.dto.workreport.WorkReportScheduleDTO;
import liuyuyang.net.model.User;
import liuyuyang.net.model.WorkReport;
import liuyuyang.net.model.WorkReportExport;
import liuyuyang.net.model.WorkReportSchedule;
import liuyuyang.net.vo.PageVo;
import liuyuyang.net.web.mapper.UserMapper;
import liuyuyang.net.web.mapper.WorkReportExportMapper;
import liuyuyang.net.web.mapper.WorkReportMapper;
import liuyuyang.net.web.mapper.WorkReportScheduleMapper;
import liuyuyang.net.web.service.WorkReportService;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional
public class WorkReportServiceImpl extends ServiceImpl<WorkReportMapper, WorkReport> implements WorkReportService {
    private static final List<String> REPORT_TYPES = Arrays.asList("daily", "weekly", "monthly");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter DISPLAY_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private WorkReportMapper workReportMapper;
    @Resource
    private WorkReportExportMapper workReportExportMapper;
    @Resource
    private WorkReportScheduleMapper workReportScheduleMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private CommonUtils commonUtils;

    @Override
    public WorkReport getCurrentUserReport(String type, String period) {
        validateType(type);
        Integer userId = getCurrentUserId();
        return getReport(userId, type, period);
    }

    @Override
    public WorkReport saveCurrentUserReport(WorkReportDTO dto) {
        validateType(dto.getType());
        Integer userId = getCurrentUserId();
        WorkReport report = null;
        if (dto.getId() != null) {
            report = workReportMapper.selectById(dto.getId());
            ensureOwner(report, userId);
        }
        if (report == null && StringUtils.hasText(dto.getPeriod())) {
            report = getReport(userId, dto.getType(), dto.getPeriod());
        }
        if (report == null) {
            report = new WorkReport();
            report.setUserId(userId);
            report.setCreateTime(nowMillis());
        }

        applyDto(report, dto);
        report.setUpdateTime(nowMillis());
        if (report.getId() == null) {
            workReportMapper.insert(report);
        } else {
            workReportMapper.updateById(report);
        }
        return report;
    }

    @Override
    public WorkReport updateCurrentUserReport(Integer id, WorkReportDTO dto) {
        Integer userId = getCurrentUserId();
        WorkReport report = workReportMapper.selectById(id);
        ensureOwner(report, userId);
        dto.setId(id);
        if (!StringUtils.hasText(dto.getType())) {
            dto.setType(report.getType());
        }
        if (!StringUtils.hasText(dto.getPeriod())) {
            dto.setPeriod(report.getPeriod());
        }
        applyDto(report, dto);
        report.setUpdateTime(nowMillis());
        workReportMapper.updateById(report);
        return report;
    }

    @Override
    public Page<WorkReport> listCurrentUserReports(String type, PageVo pageVo) {
        Integer userId = getCurrentUserId();
        LambdaQueryWrapper<WorkReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkReport::getUserId, userId);
        if (StringUtils.hasText(type)) {
            validateType(type);
            wrapper.eq(WorkReport::getType, type);
        }
        wrapper.orderByDesc(WorkReport::getUpdateTime).orderByDesc(WorkReport::getCreateTime);
        List<WorkReport> list = workReportMapper.selectList(wrapper);
        return commonUtils.getPageData(pageVo == null ? defaultPageVo() : pageVo, list);
    }

    @Override
    public WorkReportExport exportCurrentUserReport(Integer id, String source) {
        Integer userId = getCurrentUserId();
        WorkReport report = workReportMapper.selectById(id);
        ensureOwner(report, userId);
        return exportReport(report, source, getUser(userId));
    }

    @Override
    public Page<WorkReportExport> listCurrentUserExports(PageVo pageVo) {
        Integer userId = getCurrentUserId();
        LambdaQueryWrapper<WorkReportExport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkReportExport::getUserId, userId).orderByDesc(WorkReportExport::getExportTime);
        List<WorkReportExport> list = workReportExportMapper.selectList(wrapper);
        return commonUtils.getPageData(pageVo == null ? defaultPageVo() : pageVo, list);
    }

    @Override
    public List<WorkReportSchedule> getCurrentUserSchedules() {
        Integer userId = getCurrentUserId();
        ensureSchedules(userId);
        LambdaQueryWrapper<WorkReportSchedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkReportSchedule::getUserId, userId).orderByAsc(WorkReportSchedule::getId);
        return workReportScheduleMapper.selectList(wrapper);
    }

    @Override
    public List<WorkReportSchedule> updateCurrentUserSchedules(List<WorkReportScheduleDTO> schedules) {
        Integer userId = getCurrentUserId();
        ensureSchedules(userId);
        if (schedules != null) {
            for (WorkReportScheduleDTO dto : schedules) {
                validateType(dto.getType());
                WorkReportSchedule schedule = getSchedule(userId, dto.getType());
                if (schedule == null) {
                    schedule = defaultSchedule(userId, dto.getType());
                    schedule.setCreateTime(nowMillis());
                }
                if (dto.getEnabled() != null) {
                    schedule.setEnabled(dto.getEnabled());
                }
                if (StringUtils.hasText(dto.getExportTime())) {
                    schedule.setExportTime(dto.getExportTime());
                }
                if (dto.getWeeklyDay() != null) {
                    schedule.setWeeklyDay(dto.getWeeklyDay());
                }
                if (StringUtils.hasText(dto.getMonthlyMode())) {
                    schedule.setMonthlyMode(dto.getMonthlyMode());
                }
                schedule.setUpdateTime(nowMillis());
                if (schedule.getId() == null) {
                    workReportScheduleMapper.insert(schedule);
                } else {
                    workReportScheduleMapper.updateById(schedule);
                }
            }
        }
        return getCurrentUserSchedules();
    }

    @Override
    public WorkReportExport getCurrentUserExport(Integer id) {
        WorkReportExport export = workReportExportMapper.selectById(id);
        if (export == null || !Objects.equals(export.getUserId(), getCurrentUserId())) {
            throw new CustomException(404, "导出记录不存在");
        }
        return export;
    }

    @Override
    @Scheduled(fixedDelay = 60000)
    public void scanScheduledExports() {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<WorkReportSchedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkReportSchedule::getEnabled, true);
        List<WorkReportSchedule> schedules = workReportScheduleMapper.selectList(wrapper);
        for (WorkReportSchedule schedule : schedules) {
            try {
                if (!isDue(schedule, now)) {
                    continue;
                }
                String period = currentPeriod(schedule.getType(), now.toLocalDate());
                if (Objects.equals(period, schedule.getLastExportPeriod())) {
                    continue;
                }
                WorkReport report = getReport(schedule.getUserId(), schedule.getType(), period);
                if (report == null) {
                    report = emptyReport(schedule.getUserId(), schedule.getType(), period);
                    workReportMapper.insert(report);
                }
                exportReport(report, "schedule", getUser(schedule.getUserId()));
                schedule.setLastExportPeriod(period);
                schedule.setUpdateTime(nowMillis());
                workReportScheduleMapper.updateById(schedule);
            } catch (Exception ignored) {
                // One user's bad schedule should not stop the scanner for everyone else.
            }
        }
    }

    private WorkReport getReport(Integer userId, String type, String period) {
        if (!StringUtils.hasText(period)) {
            return null;
        }
        LambdaQueryWrapper<WorkReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkReport::getUserId, userId).eq(WorkReport::getType, type).eq(WorkReport::getPeriod, period);
        return workReportMapper.selectOne(wrapper);
    }

    private WorkReportExport exportReport(WorkReport report, String source, User user) {
        try {
            File dir = getExportDir();
            if (!dir.exists() && !dir.mkdirs()) {
                throw new CustomException(500, "创建导出目录失败");
            }
            String fileName = buildFileName(report, user);
            File target = new File(dir, DATE_TIME.format(LocalDateTime.now()) + "-" + fileName);
            try (XWPFDocument document = createDocumentFromTemplate(); FileOutputStream out = new FileOutputStream(target)) {
                writeDocument(document, report, user);
                document.write(out);
            }

            WorkReportExport export = new WorkReportExport();
            export.setUserId(report.getUserId());
            export.setReportId(report.getId());
            export.setType(report.getType());
            export.setPeriod(report.getPeriod());
            export.setFileName(fileName);
            export.setFilePath(target.getAbsolutePath());
            export.setSource(StringUtils.hasText(source) ? source : "manual");
            export.setExportTime(DISPLAY_DATE_TIME.format(LocalDateTime.now()));
            export.setCreateTime(nowMillis());
            workReportExportMapper.insert(export);
            return export;
        } catch (CustomException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CustomException(500, "Word 导出失败：" + ex.getMessage());
        }
    }

    private XWPFDocument createDocumentFromTemplate() throws Exception {
        ClassPathResource template = new ClassPathResource("templates/report/monthly-template.docx");
        if (!template.exists()) {
            return new XWPFDocument();
        }
        XWPFDocument document;
        try (InputStream inputStream = template.getInputStream()) {
            document = new XWPFDocument(inputStream);
        }
        List<IBodyElement> elements = document.getBodyElements();
        for (int i = elements.size() - 1; i >= 0; i--) {
            document.removeBodyElement(i);
        }
        return document;
    }

    private void writeDocument(XWPFDocument document, WorkReport report, User user) {
        writeParagraph(document, defaultTitle(report, user), "黑体", 22, true, ParagraphAlignment.CENTER);
        writeParagraph(document, " ", "宋体", 10, false, ParagraphAlignment.LEFT);
        writeSection(document, "一、工作概述", report.getSummary());
        writeSection(document, "二、重点工作详情", report.getDetails());
        writeSection(document, "三、下阶段工作计划", report.getNextPlan());
        writeSection(document, "四、附件", report.getAttachmentNote());
    }

    private void writeSection(XWPFDocument document, String heading, String body) {
        writeParagraph(document, heading, "黑体", 16, true, ParagraphAlignment.LEFT);
        List<String> lines = splitLines(body);
        if (lines.isEmpty()) {
            writeParagraph(document, "", "宋体", 12, false, ParagraphAlignment.LEFT);
            return;
        }
        for (String line : lines) {
            writeParagraph(document, line, "宋体", 12, false, ParagraphAlignment.LEFT);
        }
    }

    private void writeParagraph(XWPFDocument document, String text, String font, int fontSize, boolean bold, ParagraphAlignment alignment) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(alignment);
        paragraph.setSpacingBetween(1.5);
        paragraph.setSpacingAfter(160);
        XWPFRun run = paragraph.createRun();
        run.setFontFamily(font);
        run.setFontSize(fontSize);
        run.setBold(bold);
        run.setText(text == null ? "" : text);
    }

    private List<String> splitLines(String text) {
        List<String> lines = new ArrayList<>();
        if (!StringUtils.hasText(text)) {
            return lines;
        }
        String normalized = text.replace("[", "").replace("]", "").replace("\"", "").replace("\\n", "\n");
        for (String line : normalized.split("\\r?\\n|,")) {
            if (StringUtils.hasText(line)) {
                lines.add(line.trim());
            }
        }
        return lines;
    }

    private File getExportDir() {
        String configured = System.getenv("REPORT_EXPORT_DIR");
        String path = StringUtils.hasText(configured) ? configured : "data/reports";
        return new File(path);
    }

    private String buildFileName(WorkReport report, User user) {
        String userName = safeUserName(user);
        if ("daily".equals(report.getType())) {
            return report.getPeriod() + "工作日报（" + userName + "）.docx";
        }
        if ("weekly".equals(report.getType())) {
            String week = report.getPeriod().replace("-W", "年第") + "周";
            return week + "工作周报（" + userName + "）.docx";
        }
        YearMonth ym = YearMonth.parse(report.getPeriod());
        return ym.getYear() + "年" + String.format("%02d", ym.getMonthValue()) + "月份工作月报（" + userName + "）.docx";
    }

    private String defaultTitle(WorkReport report, User user) {
        if (StringUtils.hasText(report.getTitle())) {
            return report.getTitle();
        }
        if ("daily".equals(report.getType())) {
            return "工作日报（" + report.getPeriod() + "）";
        }
        if ("weekly".equals(report.getType())) {
            return "工作周报（" + report.getPeriod().replace("-W", "年第") + "周）";
        }
        YearMonth ym = YearMonth.parse(report.getPeriod());
        return "工作月报（" + ym.getMonthValue() + "月份）";
    }

    private boolean isDue(WorkReportSchedule schedule, LocalDateTime now) {
        if (!REPORT_TYPES.contains(schedule.getType()) || !StringUtils.hasText(schedule.getExportTime())) {
            return false;
        }
        LocalTime scheduledTime = LocalTime.parse(schedule.getExportTime());
        if (now.toLocalTime().isBefore(scheduledTime) || now.toLocalTime().isAfter(scheduledTime.plusMinutes(1))) {
            return false;
        }
        if ("weekly".equals(schedule.getType())) {
            return now.getDayOfWeek().getValue() == (schedule.getWeeklyDay() == null ? 5 : schedule.getWeeklyDay());
        }
        if ("monthly".equals(schedule.getType())) {
            return now.toLocalDate().equals(YearMonth.from(now).atEndOfMonth());
        }
        return true;
    }

    private String currentPeriod(String type, LocalDate date) {
        if ("daily".equals(type)) {
            return date.toString();
        }
        if ("weekly".equals(type)) {
            WeekFields weekFields = WeekFields.ISO;
            int week = date.get(weekFields.weekOfWeekBasedYear());
            int year = date.get(weekFields.weekBasedYear());
            return String.format(Locale.ROOT, "%d-W%02d", year, week);
        }
        return YearMonth.from(date).toString();
    }

    private WorkReport emptyReport(Integer userId, String type, String period) {
        WorkReport report = new WorkReport();
        report.setUserId(userId);
        report.setType(type);
        report.setPeriod(period);
        report.setTitle(null);
        report.setSummary("");
        report.setDetails("");
        report.setNextPlan("");
        report.setAttachmentNote("");
        report.setStatus("draft");
        report.setDraftVersion(1);
        report.setCreateTime(nowMillis());
        report.setUpdateTime(nowMillis());
        return report;
    }

    private void ensureSchedules(Integer userId) {
        for (String type : REPORT_TYPES) {
            if (getSchedule(userId, type) == null) {
                WorkReportSchedule schedule = defaultSchedule(userId, type);
                schedule.setCreateTime(nowMillis());
                schedule.setUpdateTime(nowMillis());
                workReportScheduleMapper.insert(schedule);
            }
        }
    }

    private WorkReportSchedule defaultSchedule(Integer userId, String type) {
        WorkReportSchedule schedule = new WorkReportSchedule();
        schedule.setUserId(userId);
        schedule.setType(type);
        schedule.setEnabled(false);
        schedule.setExportTime("18:00");
        schedule.setWeeklyDay(DayOfWeek.FRIDAY.getValue());
        schedule.setMonthlyMode("last_day");
        return schedule;
    }

    private WorkReportSchedule getSchedule(Integer userId, String type) {
        LambdaQueryWrapper<WorkReportSchedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkReportSchedule::getUserId, userId).eq(WorkReportSchedule::getType, type);
        return workReportScheduleMapper.selectOne(wrapper);
    }

    private void applyDto(WorkReport report, WorkReportDTO dto) {
        validateType(dto.getType());
        if (!StringUtils.hasText(dto.getPeriod())) {
            throw new CustomException(400, "报表周期不能为空");
        }
        report.setType(dto.getType());
        report.setPeriod(dto.getPeriod());
        report.setTitle(dto.getTitle());
        report.setSummary(dto.getSummary());
        report.setDetails(dto.getDetails());
        report.setNextPlan(dto.getNextPlan());
        report.setAttachmentNote(dto.getAttachmentNote());
        report.setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : "draft");
        report.setDraftVersion(dto.getDraftVersion() == null ? 1 : dto.getDraftVersion());
    }

    private void validateType(String type) {
        if (!REPORT_TYPES.contains(type)) {
            throw new CustomException(400, "报表类型不正确");
        }
    }

    private void ensureOwner(WorkReport report, Integer userId) {
        if (report == null || !Objects.equals(report.getUserId(), userId)) {
            throw new CustomException(404, "报表不存在");
        }
    }

    private User getUser(Integer userId) {
        User user = userMapper.selectById(userId);
        return user == null ? new User() : user;
    }

    private String safeUserName(User user) {
        String name = user == null ? null : user.getName();
        if (!StringUtils.hasText(name)) {
            name = user == null ? null : user.getUsername();
        }
        return StringUtils.hasText(name) ? name.replaceAll("[\\\\/:*?\"<>|]", "") : "用户";
    }

    private Integer getCurrentUserId() {
        String token = CommonUtils.getHeader("Authorization");
        if (!StringUtils.hasText(token)) {
            throw new CustomException(401, "请先登录");
        }
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Claims claims = JwtUtils.parseJWT(token);
        Object userObj = claims.get("user");
        if (userObj instanceof Map) {
            Object id = ((Map<?, ?>) userObj).get("id");
            if (id instanceof Integer) {
                return (Integer) id;
            }
            if (id instanceof BigDecimal) {
                return ((BigDecimal) id).intValue();
            }
            if (id instanceof Number) {
                return ((Number) id).intValue();
            }
            if (id != null) {
                return Integer.parseInt(String.valueOf(id));
            }
        }
        throw new CustomException(401, "无法识别当前用户");
    }

    private PageVo defaultPageVo() {
        PageVo pageVo = new PageVo();
        pageVo.setPage(1);
        pageVo.setSize(10);
        return pageVo;
    }

    private String nowMillis() {
        return String.valueOf(System.currentTimeMillis());
    }
}
