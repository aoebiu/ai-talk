package info.mengnan.aitalk.server.param;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建工具请求
 */
@Data
public class FunctionCallRequest {

    /**
     * 工具名称
     */
    @NotBlank(message = "工具名称不能为空")
    private String name;

    /**
     * 工具描述
     */
    @NotBlank(message = "工具描述不能为空")
    private String description;

    /**
     * 属性列表(JSON格式)
     */
    private String property;

    /**
     * 必需字段列表(JSON格式)
     */
    private String required;

    /**
     * 执行脚本或命令
     */
    private String execute;
}
