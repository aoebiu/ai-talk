package info.mengnan.aitalk.server.param.functionCall;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 根据用户提示词，由模型生成工具描述与属性列表的请求
 */
@Data
public class FunctionCallScriptGenerateRequest {

    @NotBlank(message = "提示词不能为空")
    private String prompt;

}
