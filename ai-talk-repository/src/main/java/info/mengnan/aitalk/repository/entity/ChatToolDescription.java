package info.mengnan.aitalk.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("chat_tool_description")
public class ChatToolDescription {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 工具名称
     */
    private String name;

    /**
     * 工具描述
     */
    private String description;

    /**
     * 属性列表(JSON格式存储)
     */
    private String property;

    /**
     * 必需字段列表(JSON格式存储)
     */
    private String required;

    /**
     * 执行脚本或命令
     */
    private String execute;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

}