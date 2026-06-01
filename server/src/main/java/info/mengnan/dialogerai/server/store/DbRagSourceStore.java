package info.mengnan.dialogerai.server.store;

import info.mengnan.dialogerai.rag.injector.RagSourceStore;
import info.mengnan.dialogerai.repository.entity.ChatMessageRagSource;
import info.mengnan.dialogerai.repository.repo.ChatMessageRagSourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DbRagSourceStore implements RagSourceStore {

    private final ChatMessageRagSourceRepository repository;

    @Override
    public void savePending(String sessionId, List<RagSource> sources) {
        if (sources == null || sources.isEmpty()) return;
        List<ChatMessageRagSource> entities = sources.stream()
                .map(s -> {
                    ChatMessageRagSource e = new ChatMessageRagSource();
                    e.setSessionId(sessionId);
                    e.setKbName(s.kbName());
                    e.setIndexName(s.indexName());
                    e.setContent(s.text());
                    return e;
                })
                .toList();
        repository.batchInsert(entities);
    }

    @Override
    public void linkToMessage(String sessionId, Long messageId) {
        repository.linkToMessage(sessionId, messageId);
    }
}
