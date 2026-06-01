package info.mengnan.dialogerai.server.param.knowledgebase;

import lombok.Data;

@Data
public class KnowledgeBaseUpdateRequest {

    private String name;
    private String description;
    private String visibility;
}
