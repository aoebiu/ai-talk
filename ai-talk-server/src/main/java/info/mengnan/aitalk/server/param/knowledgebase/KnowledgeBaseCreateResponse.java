package info.mengnan.aitalk.server.param.knowledgebase;

import info.mengnan.aitalk.repository.enums.KnowledgeBaseStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class KnowledgeBaseCreateResponse {

    private Long id;
    private String name;
    private String indexName;
    private KnowledgeBaseStatus status;
}
