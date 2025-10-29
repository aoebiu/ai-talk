package info.mengnan.aitalk.rag;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.service.*;

import java.util.List;

public interface AssistantUnique {

    @Moderate
    @SystemMessage(fromResource = "rag/customer_message.txt")
    TokenStream chatStreaming(@MemoryId String memoryId, @UserMessage String userMessage);

}
