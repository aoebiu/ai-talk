package info.mengnan.aitalk.repository.async;

/**
 * {@code async_task.steps} JSON 数组中单步的结构定义。
 */
public record AsyncTaskStepDetail(int step, String label, AsyncTaskStepStatus status) {

    public static AsyncTaskStepDetail pending(int step, String label) {
        return new AsyncTaskStepDetail(step, label, AsyncTaskStepStatus.PENDING);
    }
}
