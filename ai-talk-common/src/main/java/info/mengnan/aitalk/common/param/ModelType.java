package info.mengnan.aitalk.common.param;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ModelType {

    CHAT,
    STREAMING_CHAT,
    EMBEDDING,
    SCORING,
    MODERATE
    ;


    public String n() {
        return this.name().toLowerCase();
    }
}
