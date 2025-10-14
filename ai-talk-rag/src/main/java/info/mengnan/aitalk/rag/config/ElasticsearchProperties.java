package info.mengnan.aitalk.rag.config;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ElasticsearchProperties {
    private String host = "localhost";
    private Integer port = 9200;
    private String username;
    private String password;

    /**
     * 默认索引名称（用于向后兼容）
     * 如果配置文件中指定了 indexName，将在动态扫描的基础上确保该索引存在
     */
    private String indexName = "ai-talk-vectors";

    /**
     * 可选：手动指定要管理的索引列表
     * 如果为空，则会自动扫描所有 ES 索引
     */
    private List<String> indexNames = new ArrayList<>();

    /**
     * 是否启用自动扫描所有索引
     * 默认为 true，会自动发现所有非系统索引
     */
    private boolean autoDiscoverIndices = true;

    /**
     * 索引名称过滤模式（正则表达式）
     * 仅注册匹配此模式的索引，默认为 null（不过滤）
     */
    private String indexNamePattern;
}