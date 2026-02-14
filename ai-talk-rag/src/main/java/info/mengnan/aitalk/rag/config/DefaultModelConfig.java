package info.mengnan.aitalk.rag.config;

import lombok.Data;

@Data
public class DefaultModelConfig {

    private String modelName = "qwen3-vl-plus";
    private String compressModelName = "qwen-turbo";

    public static final Long DEFAULT_OPTION_ID = 1L;
    public static final String DEFAULT_SESSION = "default-session";

}
