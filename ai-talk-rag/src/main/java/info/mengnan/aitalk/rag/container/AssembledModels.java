package info.mengnan.aitalk.rag.container;

import info.mengnan.aitalk.rag.config.ModelConfig;

public record AssembledModels(String name,  // 模型名称
                              Boolean tools, // 是否开启tools
                              Boolean rag,  // 是否开启rag
                              Integer maxMessages, // 最大消息数量
                              String transform,    // transform类型
                              Boolean contentAggregator, // 是否开启聚合排序
                              String contentInjectorPrompt, // 提示词模板
                              Integer maxResults,  // 最大检索数
                              Double minScore, // 检索最小相似度分数
                              Boolean inDB,
                              ModelConfig chatModel,
                              ModelConfig streamingChatModel,
                              ModelConfig embeddingModel,
                              ModelConfig scoringModel) {

}