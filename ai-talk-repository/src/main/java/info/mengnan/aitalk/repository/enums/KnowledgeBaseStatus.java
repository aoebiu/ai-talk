package info.mengnan.aitalk.repository.enums;

import lombok.Getter;

@Getter
public enum KnowledgeBaseStatus {

    DRAFT("DRAFT"),
    ACTIVE("ACTIVE");

    private final String value;

    KnowledgeBaseStatus(String value) {
        this.value = value;
    }
}