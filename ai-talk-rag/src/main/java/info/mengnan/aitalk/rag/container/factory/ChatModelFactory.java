package info.mengnan.aitalk.rag.container.factory;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import info.mengnan.aitalk.rag.config.ModelConfig;

import static info.mengnan.aitalk.common.param.ModelType.CHAT;
import static info.mengnan.aitalk.common.param.ModelType.STREAMING_CHAT;

/**
 * 聊天模型工厂接口
 * 提供创建聊天模型和流式聊天模型的能力
 */
public interface ChatModelFactory extends ModelFactory {

    /**
     * 创建聊天模型
     *
     * @param modelConfig 模型配置
     * @return ChatModel 实例
     * @throws UnsupportedOperationException 如果不支持该提供商或模型类型
     * @throws RuntimeException 如果创建失败
     */
    default ChatModel createChatModel(ModelConfig modelConfig) {
        return (ChatModel) createModel(modelConfig, CHAT);
    }

    /**
     * 创建流式聊天模型
     *
     * @param modelConfig 模型配置
     * @return StreamingChatModel 实例
     * @throws UnsupportedOperationException 如果不支持该提供商或模型类型
     * @throws RuntimeException 如果创建失败
     */
    default StreamingChatModel createStreamingChatModel(ModelConfig modelConfig) {
        return (StreamingChatModel) createModel(modelConfig, STREAMING_CHAT);
    }
}
