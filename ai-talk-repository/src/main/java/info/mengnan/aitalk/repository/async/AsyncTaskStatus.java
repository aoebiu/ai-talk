package info.mengnan.aitalk.repository.async;

/**
 * 异步任务整体状态，与库字段 {@code async_task.status} 一致。
 */
public enum AsyncTaskStatus {

    PENDING,
    RUNNING,
    COMPLETED,
    FAILED
}
