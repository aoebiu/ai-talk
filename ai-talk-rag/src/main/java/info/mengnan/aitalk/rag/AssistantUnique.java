package info.mengnan.aitalk.rag;

import dev.langchain4j.service.*;
import dev.langchain4j.service.memory.ChatMemoryAccess;

public interface AssistantUnique extends ChatMemoryAccess{

    @Moderate
    @SystemMessage(fromResource = "rag/customer_message.txt")
    TokenStream chatStreaming(@MemoryId String memoryId, @UserMessage String userMessage);

}
