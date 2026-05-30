package info.mengnan.aitalk.repository.enums;

/**
 * 文档处理状态枚举，与 {@code document_info.status} 字段值对应。
 */
public enum DocumentStatus {

    /** 已上传，等待处理 */
    PENDING,

    /** 阶段1：正在解析文件内容 */
    PARSING,

    /** 阶段2：正在清洗文本 */
    CLEANING,

    /** 阶段3：正在分块 */
    CHUNKING,

    /** 阶段4：正在向量化存储 */
    EMBEDDING,

    /** 全部完成 */
    DONE,

    /** 某阶段失败 */
    FAILED
}
