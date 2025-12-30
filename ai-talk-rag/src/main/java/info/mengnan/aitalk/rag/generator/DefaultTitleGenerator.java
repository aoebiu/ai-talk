package info.mengnan.aitalk.rag.generator;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.content.Content;
import info.mengnan.aitalk.rag.constant.promptTemplate.PromptTemplateConstant;

import java.util.HashMap;
import java.util.Map;

public class DefaultTitleGenerator implements TitleGenerator {

    private final PromptTemplate promptTemplate;

    public DefaultTitleGenerator(PromptTemplate promptTemplate) {
        if (promptTemplate == null) {
            promptTemplate = PromptTemplateConstant.TITLE_GENERATION_PROMPT_TEMPLATE;
        }
        this.promptTemplate = promptTemplate;
    }

    protected Prompt createPrompt(String content) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("query", content);
        return promptTemplate.apply(variables);
    }


    @Override
    public ChatMessage generate(Content content) {
        Prompt prompt = createPrompt(content.textSegment().text());
        return prompt.toUserMessage();
    }
}