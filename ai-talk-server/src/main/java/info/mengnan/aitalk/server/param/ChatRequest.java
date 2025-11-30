package info.mengnan.aitalk.server.param;

import lombok.Data;

@Data
public class ChatRequest {

    private String sessionId;
    private Long optionId;
    private String message;
    private Long memberId;
    private boolean inDB = true;

}
