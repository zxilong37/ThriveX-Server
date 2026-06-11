package liuyuyang.net.model;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@TableName("knowledge_chunk")
public class KnowledgeChunk extends BaseModel {
    @ApiModelProperty(value = "知识来源 ID")
    private Integer sourceId;

    @ApiModelProperty(value = "分块标题")
    private String title;

    @ApiModelProperty(value = "分块正文")
    private String content;

    @ApiModelProperty(value = "知识来源路径")
    private String sourcePath;

    @ApiModelProperty(value = "知识来源类型")
    private String sourceType;

    @ApiModelProperty(value = "标签")
    private String tags;

    @ApiModelProperty(value = "分块顺序")
    private Integer chunkOrder;

    @ApiModelProperty(value = "内容摘要 Hash")
    private String contentHash;
}
