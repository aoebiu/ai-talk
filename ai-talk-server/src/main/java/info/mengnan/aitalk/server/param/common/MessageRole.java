package info.mengnan.aitalk.server.param.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MessageRole {

    USER,      // 用户提示词
    ASSISTANT, // 模型提示词
    COMPRESS,  // 压缩提示词
    SYSTEM,    // 系统提示词
    ;

    public boolean equals(String role) {
        return this.name().equals(role);
    }

    public String n() {
        return this.name();
    }
}
