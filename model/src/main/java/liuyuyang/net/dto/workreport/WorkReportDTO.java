package liuyuyang.net.dto.workreport;

import lombok.Data;

@Data
public class WorkReportDTO {
    private Integer id;
    private String type;
    private String period;
    private String title;
    private String summary;
    private String details;
    private String nextPlan;
    private String attachmentNote;
    private String status;
    private Integer draftVersion;
}
