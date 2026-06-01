package info.mengnan.dialogerai.server.param.chat;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ChatSessionResponse {

    public static final String DEFAULT_TITLE = "新对话";

    private String sessionId;
    private String title;
    private LocalDateTime lastModified;

}
