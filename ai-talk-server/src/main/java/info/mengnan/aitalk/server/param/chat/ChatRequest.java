package info.mengnan.aitalk.server.param.chat;

import lombok.Data;

@Data
public class ChatRequest {

    private String sessionId;
    private Long optionId;
    private String message;
    private Long memberId;
    private boolean inDB = true;
    /** 若非 null，流式回复前先删除该消息及之后的所有消息（用于重新生成 / 编辑后重发） */
    private Long fromMessageId;

}
