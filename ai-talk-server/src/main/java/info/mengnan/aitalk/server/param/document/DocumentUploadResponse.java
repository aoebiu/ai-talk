package info.mengnan.aitalk.server.param.document;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DocumentUploadResponse {

    private Long documentId;
    private Long kbId;
    private String taskId;
    private String originalName;
}
