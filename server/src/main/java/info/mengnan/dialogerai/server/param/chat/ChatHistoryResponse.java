package info.mengnan.dialogerai.server.param.chat;

import info.mengnan.dialogerai.repository.entity.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class ChatHistoryResponse {

    private List<ChatMessage> messages;

    private Map<Long, List<Long>> ragSourceMap;
}
