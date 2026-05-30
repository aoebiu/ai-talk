package info.mengnan.aitalk.repository.enums;

/**
 * 异步任务类型，与库字段 {@code async_task.task_type} 一致。
 */
public enum AsyncTaskType {

    /** 两阶段生成 Function Call 工具脚本与元数据 */
    GENERATE_SCRIPT,

    /** 文档处理：解析 → 清洗 → 分块 → 向量化 */
    DOC_PROCESS,

    /** 知识库维度构建：聚合库内所有文档的四步流水线进度 */
    KB_BUILD
}
