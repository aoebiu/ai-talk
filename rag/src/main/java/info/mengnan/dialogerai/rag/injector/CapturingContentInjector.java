package info.mengnan.dialogerai.rag.injector;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.injector.ContentInjector;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;

import java.util.List;

import static info.mengnan.dialogerai.rag.constant.promptTemplate.PromptTemplateConstant.CONTENT_INJECTOR_PROMPT_TEMPLATE;

/**
 * 在调用 DefaultContentInjector 注入之前，将命中的知识库片段通过 {@link RagSourceStore} 持久化
 * 使用 session_id 关联，由 PersistentChatMemoryStore 在保存用户消息后补写 message_id
 */
public class CapturingContentInjector implements ContentInjector {

    private static final String INDEX_NAME_KEY = "indexName";
    private static final String KB_NAME_KEY = "kbName";
    private final DefaultContentInjector delegate =
            new DefaultContentInjector(CONTENT_INJECTOR_PROMPT_TEMPLATE);

    private final String sessionId;
    private final RagSourceStore ragSourceStore;

    public CapturingContentInjector(String sessionId, RagSourceStore ragSourceStore) {
        this.sessionId = sessionId;
        this.ragSourceStore = ragSourceStore;
    }

    @Override
    public ChatMessage inject(List<Content> contents, ChatMessage userMessage) {
        if (contents.isEmpty()) {
            return userMessage;
        }

        List<RagSourceStore.RagSource> sources = contents.stream()
                .map(c -> {
                    String kbName = c.textSegment().metadata().getString(KB_NAME_KEY);
                    String indexName = c.textSegment().metadata().getString(INDEX_NAME_KEY);
                    if (kbName == null || kbName.isBlank()) {
                        kbName = indexName;
                    }
                    return new RagSourceStore.RagSource(kbName, indexName, c.textSegment().text());
                })
                .toList();
        ragSourceStore.savePending(sessionId, sources);

        return delegate.inject(contents, userMessage);
    }
}
