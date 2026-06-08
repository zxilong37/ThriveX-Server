package liuyuyang.net.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("work_report_schedule")
public class WorkReportSchedule extends BaseModel {
    private Integer userId;
    private String type;
    private Boolean enabled;
    private String exportTime;
    private Integer weeklyDay;
    private String monthlyMode;
    private String lastExportPeriod;
    private String updateTime;
}
