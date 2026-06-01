package info.mengnan.dialogerai.server.param.document;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class DocumentContentResponse {

    private Long documentId;
    private String originalName;
    private String status;
    private Integer segmentCount;
    private String content;
    private List<String> segments;
}
