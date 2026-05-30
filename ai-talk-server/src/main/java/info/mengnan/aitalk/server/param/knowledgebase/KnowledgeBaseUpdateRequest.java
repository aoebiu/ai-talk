package info.mengnan.aitalk.server.param.knowledgebase;

import lombok.Data;

@Data
public class KnowledgeBaseUpdateRequest {

    private String name;
    private String description;
    private String visibility;
}
