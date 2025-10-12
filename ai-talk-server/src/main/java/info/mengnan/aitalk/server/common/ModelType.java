package info.mengnan.aitalk.server.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ModelType {

    CHAT,
    STREAMING_CHAT,
    EMBEDDING,
    SCORING,
    ;


    public String n() {
        return this.name().toLowerCase();
    }
}
