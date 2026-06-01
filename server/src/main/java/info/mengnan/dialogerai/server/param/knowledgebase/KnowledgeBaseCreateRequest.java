package info.mengnan.dialogerai.server.param.knowledgebase;

import lombok.Data;

@Data
public class KnowledgeBaseCreateRequest {

    private String name;

    private String description;

    /** private / public */
    private String visibility;
}
