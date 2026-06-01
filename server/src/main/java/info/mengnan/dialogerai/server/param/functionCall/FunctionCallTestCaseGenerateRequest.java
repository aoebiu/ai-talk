package info.mengnan.dialogerai.server.param.functionCall;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 批量测试用例生成请求
 */
@Data
public class FunctionCallTestCaseGenerateRequest {

    /**
     * 生成测试用例数量（1~50）
     */
    @Length(min = 1, max = 50)
    private int count = 1;
}
