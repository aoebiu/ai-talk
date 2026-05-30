package info.mengnan.aitalk.repository.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 单步状态，序列化为小写字符串以兼容前端与既有 JSON。
 */
public enum AsyncTaskStepStatus {

    PENDING("pending"),
    RUNNING("running"),
    COMPLETED("completed");

    private final String jsonValue;

    AsyncTaskStepStatus(String jsonValue) {
        this.jsonValue = jsonValue;
    }

    @JsonValue
    public String getJsonValue() {
        return jsonValue;
    }

    @JsonCreator
    public static AsyncTaskStepStatus fromJson(String value) {
        if (value == null) {
            return null;
        }
        for (AsyncTaskStepStatus s : values()) {
            if (s.jsonValue.equals(value)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown async task step status: " + value);
    }
}
