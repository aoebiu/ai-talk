package info.mengnan.aitalk.server.param.chat;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ChatSessionResponse {

    private String sessionId;
    private String title;
    private LocalDateTime lastModified;

}
