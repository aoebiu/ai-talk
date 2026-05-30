package info.mengnan.aitalk.repository.repo;

import info.mengnan.aitalk.repository.entity.ChatMessageRagSource;
import info.mengnan.aitalk.repository.mapper.ChatMessageRagSourceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ChatMessageRagSourceRepository {

    private final ChatMessageRagSourceMapper mapper;

    public void batchInsert(List<ChatMessageRagSource> list) {
        list.forEach(mapper::insert);
    }

    public void linkToMessage(String sessionId, Long messageId) {
        mapper.linkToMessage(sessionId, messageId);
    }

    public List<ChatMessageRagSource> find(List<Long> ids) {
        return mapper.find(ids);
    }

    public List<Long> findIdsForMessage(Long messageId) {
        return mapper.findForMessage(messageId).stream()
                .map(ChatMessageRagSource::getId)
                .toList();
    }

    public Map<Long, List<Long>> findRagSourceIdMap(String sessionId) {
        return mapper.findIdAndMessageId(sessionId).stream()
                .collect(Collectors.groupingBy(
                        ChatMessageRagSource::getMessageId,
                        Collectors.mapping(ChatMessageRagSource::getId, Collectors.toList())
                ));
    }

    public Map<Long, List<ChatMessageRagSource>> findGroupedByMessage(String sessionId) {
        return mapper.find(sessionId).stream()
                .collect(Collectors.groupingBy(ChatMessageRagSource::getMessageId));
    }

    public List<ChatMessageRagSource> findPending(String sessionId) {
        return mapper.findPending(sessionId);
    }

    public void deleteByMessageIdGreaterThanOrEqual(String sessionId, Long messageId) {
        mapper.deleteByMessageIdGreaterThanOrEqual(sessionId, messageId);
    }
}
