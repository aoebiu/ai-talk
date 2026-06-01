package info.mengnan.dialogerai.kb.core;


import dev.langchain4j.rag.content.retriever.ContentRetriever;

import java.util.List;
import java.util.Map;

/**
 * 解析用户下可用于 RAG 检索的知识库索引列表。
 */
public interface KnowledgeBaseIndexResolver {

    List<KbIndexRef> resolveActiveIndexes(Long memberId);

    record KbIndexRef(String indexName, String displayName) {}
}
