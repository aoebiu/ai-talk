package info.mengnan.aitalk.server.param.chat;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChatSessionResponse {

    private String sessionId;
    private String title;

}
