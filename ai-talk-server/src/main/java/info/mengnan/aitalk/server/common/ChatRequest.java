package info.mengnan.aitalk.server.common;

import lombok.Data;

@Data
public class ChatRequest {

    private String sessionId;
    private Long optionId;
    private String message;

}
