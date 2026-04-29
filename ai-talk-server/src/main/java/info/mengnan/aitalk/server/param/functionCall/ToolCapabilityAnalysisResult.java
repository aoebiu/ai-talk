package info.mengnan.aitalk.server.param.functionCall;

import lombok.Data;

/**
 * 判断工具需要哪些运行时能力
 */
@Data
public class ToolCapabilityAnalysisResult {

    private boolean needsHttp;
    private boolean needsConfig;
    private boolean needsJwt;
}
