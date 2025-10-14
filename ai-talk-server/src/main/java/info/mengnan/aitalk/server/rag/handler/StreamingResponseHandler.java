package info.mengnan.aitalk.server.rag.handler;

/**
 * 流式响应处理器接口
 * 用于解耦 ChatService 与 Flux,使服务层不依赖于响应式框架
 */
public interface StreamingResponseHandler {

    /**
     * 处理部分响应 token
     *
     * @param token 响应的部分文本
     */
    void onToken(String token);

    /**
     * 响应完成时调用
     *
     * @param completeResponse 完整的响应文本
     */
    void onComplete(String completeResponse);

    /**
     * 发生错误时调用
     *
     * @param error 错误对象
     */
    void onError(Throwable error);

    /**
     * 检查处理器是否已取消
     *
     * @return true 如果已取消
     */
    boolean isCancelled();
}