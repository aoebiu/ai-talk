package info.mengnan.aitalk.server.core;

import info.mengnan.aitalk.kb.core.KnowledgeBaseIndexResolver;
import info.mengnan.aitalk.repository.repo.KnowledgeBaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DbKnowledgeBaseIndexResolver implements KnowledgeBaseIndexResolver {

    private final KnowledgeBaseRepository knowledgeBaseRepository;

    @Override
    public List<KbIndexRef> resolveActiveIndexes(Long memberId) {
        return knowledgeBaseRepository.findActiveByMemberId(memberId).stream()
                .map(kb -> new KbIndexRef(kb.getIndexName(), kb.getName()))
                .toList();
    }
}
