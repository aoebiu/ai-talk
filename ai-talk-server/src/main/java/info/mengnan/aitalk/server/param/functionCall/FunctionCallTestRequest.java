package info.mengnan.aitalk.server.param.functionCall;

import lombok.Data;

/**
 * 测试工具执行请求
 */
@Data
public class FunctionCallTestRequest {

    /**
     * 测试参数 (JSON 格式)
     */
    private String parameters;
}