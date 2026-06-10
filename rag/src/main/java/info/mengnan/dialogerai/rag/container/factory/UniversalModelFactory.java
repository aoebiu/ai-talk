package info.mengnan.dialogerai.rag.container.factory;

/**
 * 聚合接口
 * 实现此接口的类并不直接创建模型，而是将请求分派给具体 Provider 的工厂实现。
 */
public interface UniversalModelFactory extends ChatModelFactory,
                                               EmbeddingModelFactory,
                                               ScoringModelFactory,
                                               ModerationModelFactory,
                                               ImageModelFactory {
}
