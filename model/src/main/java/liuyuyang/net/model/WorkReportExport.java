package liuyuyang.net.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("work_report_export")
public class WorkReportExport extends BaseModel {
    private Integer userId;
    private Integer reportId;
    private String type;
    private String period;
    private String fileName;
    private String filePath;
    private String source;
    private String exportTime;
}
