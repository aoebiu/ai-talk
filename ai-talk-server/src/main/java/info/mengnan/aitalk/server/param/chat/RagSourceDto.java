package info.mengnan.aitalk.server.param.chat;

import info.mengnan.aitalk.repository.entity.ChatMessageRagSource;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RagSourceDto {

    private Long id;
    private String kbName;
    private String indexName;
    private String text;

    public static RagSourceDto from(ChatMessageRagSource source) {
        return new RagSourceDto(source.getId(), source.getKbName(), source.getIndexName(), source.getContent());
    }
}
