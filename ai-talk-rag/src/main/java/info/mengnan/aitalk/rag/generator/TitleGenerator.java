package info.mengnan.aitalk.rag.generator;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.rag.content.Content;

public interface TitleGenerator {

    /**
     * Generate a title based on the input content
     * @param content the input content to summarize
     * @return generated title (under 10 words)
     */
    ChatMessage generate(Content content);

}