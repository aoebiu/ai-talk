package info.mengnan.dialogerai.server.param.knowledgebase;

import info.mengnan.dialogerai.repository.entity.KnowledgeBase;
import info.mengnan.dialogerai.repository.enums.KnowledgeBaseStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgeBaseResponse {

    private Long id;
    private String name;
    private String description;
    private String visibility;
    private KnowledgeBaseStatus status;
    private String indexName;
    private Long documentCount;
    private String buildTaskId;
    private LocalDateTime createdAt;

    public static KnowledgeBaseResponse from(KnowledgeBase kb, long documentCount) {
        KnowledgeBaseResponse resp = new KnowledgeBaseResponse();
        resp.setId(kb.getId());
        resp.setName(kb.getName());
        resp.setDescription(kb.getDescription());
        resp.setVisibility(kb.getVisibility());
        resp.setStatus(kb.getStatus());
        resp.setIndexName(kb.getIndexName());
        resp.setDocumentCount(documentCount);
        resp.setBuildTaskId(kb.getBuildTaskId());
        resp.setCreatedAt(kb.getCreatedAt());
        return resp;
    }
}
