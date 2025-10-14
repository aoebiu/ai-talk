package info.mengnan.aitalk.rag.config;

import lombok.Data;

/**
 * 聊天选项配置
 */
@Data
public class ChatOptionConfig {
    /**
     * 配置名称
     */
    private String name;

    /**
     * 是否启用工具
     */
    private Boolean tools;

    /**
     * 是否启用RAG
     */
    private Boolean rag;

    /**
     * 最大消息数
     */
    private Integer maxMessages;

    /**
     * 转换类型
     */
    private String transform;

    /**
     * 是否启用内容聚合器
     */
    private Boolean contentAggregator;

    /**
     * 内容注入器提示词
     */
    private String contentInjectorPrompt;

    /**
     * 最大检索结果数
     */
    private Integer maxResults;

    /**
     * 最小相似度分数
     */
    private Double minScore;

    /**
     * 是否存储在数据库
     */
    private Boolean inDB;

    public ChatOptionConfig() {
    }
}