package liuyuyang.net.model;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@TableName("knowledge_source")
public class KnowledgeSource extends BaseModel {
    @ApiModelProperty(value = "知识来源名称")
    private String name;

    @ApiModelProperty(value = "知识来源类型")
    private String sourceType;

    @ApiModelProperty(value = "知识来源路径")
    private String sourcePath;

    @ApiModelProperty(value = "知识来源说明")
    private String description;

    @ApiModelProperty(value = "是否启用")
    private Integer enabled;

    @ApiModelProperty(value = "分块数量")
    private Integer chunkCount;

    @ApiModelProperty(value = "索引状态")
    private String indexStatus;

    @ApiModelProperty(value = "最后索引时间")
    private String lastIndexedTime;

    @ApiModelProperty(value = "更新时间")
    private String updateTime;
}
