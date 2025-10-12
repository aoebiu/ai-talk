package info.mengnan.aitalk.server.rag.injector;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.injector.ContentInjector;
import info.mengnan.aitalk.server.util.Cast;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.langchain4j.internal.Utils.isNotNullOrBlank;


public class DefaultContentInjector implements ContentInjector {

    public static final PromptTemplate DEFAULT_PROMPT_TEMPLATE = PromptTemplate.from(
            """
                        {{userMessage}}

                        使用以下信息进行回答:
                        {{contents}}
                    """
    );

    private final PromptTemplate promptTemplate;

    public DefaultContentInjector(PromptTemplate promptTemplate) {
        if (promptTemplate == null) {
            promptTemplate = DEFAULT_PROMPT_TEMPLATE;
        }
        this.promptTemplate = promptTemplate;
    }

    protected Prompt createPrompt(UserMessage userMessage, List<Content> contents) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("userMessage", userMessage.singleText());
        variables.put("contents", contents);
        return promptTemplate.apply(variables);
    }

    @Override
    public ChatMessage inject(List<Content> contents, ChatMessage chatMessage) {

        if (contents.isEmpty()) {
            return chatMessage;
        }

        Prompt prompt = createPrompt(Cast.cast(chatMessage), contents);
        UserMessage message = (UserMessage) chatMessage;
        if (isNotNullOrBlank(message.name())) {
            return prompt.toUserMessage(message.name());
        }

        return prompt.toUserMessage();
    }

}
