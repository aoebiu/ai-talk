package info.mengnan.dialogerai.common.param;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ModelType {

    CHAT,
    STREAMING_CHAT,
    EMBEDDING,
    SCORING,
    MODERATE,
    IMAGE
    ;


    public String n() {
        return this.name().toLowerCase();
    }
}
