package info.mengnan.aitalk.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import java.time.LocalDateTime;


@Data
@TableName("chat_option")
public class ChatOption {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 配置名称
     */
    private String name;

    /**
     * 最大消息窗口数
     */
    private Integer maxMessages;

    /**
     * 是否启用
     */
    private Boolean enabled;

    private Boolean rag;

    /**
     * Query Transformer 类型
     */
    private String transform;

    /**
     * Content Aggregator 类型
     */
    private Boolean contentAggregator;

    private Boolean tools;

    /**
     * Content Injector 提示词模板
     */
    private String contentInjectorPrompt;

    /**
     * 检索最大结果数
     */
    private Integer maxResults;

    /**
     * 检索最小相似度分数
     */
    private Double minScore;

    @TableField("in_DB")
    private Boolean inDB;

    /**
     * 备注
     */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

}