package info.mengnan.aitalk.server.param.functionCall;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 根据工具描述与入参，由模型生成 execute 脚本的请求（字段与创建工具接口对齐，便于复用表单）
 */
@Data
public class FunctionCallScriptGenerateRequest {

    @NotBlank(message = "工具名称不能为空")
    private String name;

    @NotBlank(message = "工具描述不能为空")
    private String description;

    /** 参数 JSON Schema（与 {@link FunctionCallRequest#getProperty()} 一致） */
    private String property;

    /** 必填字段列表 JSON（与 {@link FunctionCallRequest#getRequired()} 一致） */
    private String required;

}
