package info.mengnan.aitalk.rag.injector;

import java.util.List;

/**
 * RAG 源持久化接口
 */
public interface RagSourceStore {

    /**
     * inject() 阶段调用，message_id 尚未生成，仅记录 session_id
     */
    void savePending(String sessionId, List<RagSource> sources);

    /**
     * updateMessages() 阶段调用，将 pending 记录与消息 ID 绑定
     */
    void linkToMessage(String sessionId, Long messageId);

    /**
     * RAG 检索命中的知识库片段
     */
    record RagSource(String kbName, String indexName, String text) {}

}
