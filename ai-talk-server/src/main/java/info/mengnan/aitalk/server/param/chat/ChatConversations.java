package info.mengnan.aitalk.server.param.chat;

import lombok.Data;

@Data
public class ChatConversations {
    private String sessionId;
    private Long memberId;
    private String title;


    public ChatConversations(Long memberId, String sessionId) {
        this.sessionId = sessionId;
        this.memberId = memberId;
    }
}
