package info.mengnan.aitalk.server.param.functionCall;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * AI 生成工具元数据的响应
 */
@Data
public class FunctionCallGenerateResponse {

    private String name;

    private String description;

    /** 属性列表，key 为参数名，value 包含 type 和 description */
    private Map<String, PropertyDetail> properties;

    /** 必填参数名列表 */
    private List<String> required;

    /** 执行脚本 */
    private String execute;

    @Data
    public static class PropertyDetail {
        private String type;
        private String description;
    }
}
