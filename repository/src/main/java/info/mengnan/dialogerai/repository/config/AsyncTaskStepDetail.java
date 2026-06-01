package info.mengnan.dialogerai.repository.config;

import info.mengnan.dialogerai.repository.enums.AsyncTaskStepStatus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * {@code async_task.steps} JSON 数组中单步的结构定义。
 */
public record AsyncTaskStepDetail(
        int step,
        String label,
        AsyncTaskStepStatus status,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        Long durationMs,
        Map<String, Object> summary) {

    public static AsyncTaskStepDetail pending(int step, String label) {
        return new AsyncTaskStepDetail(step, label, AsyncTaskStepStatus.PENDING, null, null, null, null);
    }

    /** 将步骤标记为进行中，记录开始时间 */
    public AsyncTaskStepDetail start() {
        return new AsyncTaskStepDetail(step, label, AsyncTaskStepStatus.RUNNING, LocalDateTime.now(), null, null, null);
    }

    /** 将步骤标记为已完成，记录结束时间、耗时和摘要 */
    public AsyncTaskStepDetail complete(Map<String, Object> summary) {
        LocalDateTime now = LocalDateTime.now();
        long ms = startedAt != null ? Duration.between(startedAt, now).toMillis() : 0L;
        return new AsyncTaskStepDetail(step, label, AsyncTaskStepStatus.COMPLETED, startedAt, now, ms, summary);
    }

    /** 仅变更状态，保留其他字段不变 */
    public AsyncTaskStepDetail withStatus(AsyncTaskStepStatus newStatus) {
        return new AsyncTaskStepDetail(step, label, newStatus, startedAt, finishedAt, durationMs, summary);
    }
}
