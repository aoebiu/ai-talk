package info.mengnan.dialogerai.server.param.knowledgebase;

import info.mengnan.dialogerai.repository.enums.KnowledgeBaseStatus;
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
