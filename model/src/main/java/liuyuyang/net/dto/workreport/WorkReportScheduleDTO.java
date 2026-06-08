package liuyuyang.net.dto.workreport;

import lombok.Data;

@Data
public class WorkReportScheduleDTO {
    private String type;
    private Boolean enabled;
    private String exportTime;
    private Integer weeklyDay;
    private String monthlyMode;
}
