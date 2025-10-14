package info.mengnan.aitalk.rag.container.factory;

import lombok.Getter;

/**
 * LLM模型提供商枚举
 */
@Getter
public enum ModelProvider {

    DASHSCOPE("Qwen", "阿里云DashScope"),
    OPENAI("OpenAI", "OpenAI"),
    OLLAMA("Ollama", "Ollama"),
    ONNX("ONNX", "ONNX Runtime");

    private final String code;
    private final String name;

    ModelProvider(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static ModelProvider fromCode(String code) {
        for (ModelProvider provider : values()) {
            if (provider.code.equals(code)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("Unknown model provider code: " + code);
    }
}