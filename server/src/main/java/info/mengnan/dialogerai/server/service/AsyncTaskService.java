package info.mengnan.dialogerai.server.service;

import info.mengnan.dialogerai.common.util.JSONUtil;
import info.mengnan.dialogerai.repository.enums.AsyncTaskStatus;
import info.mengnan.dialogerai.repository.config.AsyncTaskStepDetail;
import info.mengnan.dialogerai.repository.enums.AsyncTaskStepStatus;
import info.mengnan.dialogerai.repository.enums.AsyncTaskType;
import info.mengnan.dialogerai.repository.entity.AsyncTask;
import info.mengnan.dialogerai.repository.repo.AsyncTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncTaskService {

    private final AsyncTaskRepository asyncTaskRepository;

    private static final ThreadPoolExecutor POOL_EXECUTOR = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors(),
            Runtime.getRuntime().availableProcessors() * 2,
            2, TimeUnit.MINUTES,
            new LinkedBlockingDeque<>(256),
            new ThreadPoolExecutor.CallerRunsPolicy());

    /**
     * 创建异步任务，写入数据库，状态为 PENDING
     *
     * @param memberId   用户 ID
     * @param taskType   任务类型
     * @param stepLabels 各步骤标签，如 ["能力分析", "生成工具元数据"]
     * @return taskId
     */
    public String createTask(Long memberId, AsyncTaskType taskType, List<String> stepLabels) {
        String taskId = UUID.randomUUID().toString().replace("-", "");

        List<AsyncTaskStepDetail> steps = new ArrayList<>(stepLabels.size());
        for (int i = 0; i < stepLabels.size(); i++) {
            steps.add(AsyncTaskStepDetail.pending(i + 1, stepLabels.get(i)));
        }

        AsyncTask task = new AsyncTask();
        task.setTaskId(taskId);
        task.setMemberId(memberId);
        task.setTaskType(taskType);
        task.setStatus(AsyncTaskStatus.PENDING);
        task.setCurrentStep(0);
        task.setTotalSteps(stepLabels.size());
        task.setSteps(JSONUtil.toJsonStr(steps));

        asyncTaskRepository.insert(task);
        return taskId;
    }

    /**
     * 提交任务到线程池异步执行
     */
    public void submitTask(String taskId, Runnable taskLogic) {
        POOL_EXECUTOR.execute(() -> {
            try {
                taskLogic.run();
            } catch (Exception e) {
                log.error("异步任务执行异常, taskId={}", taskId, e);
                failTask(taskId, e.getMessage());
            }
        });
    }

    /**
     * 标记某步骤为 running：序号小于当前步的置为 completed，当前步为 running，更大的仍为 pending。
     * 同时将任务状态置为 RUNNING。
     */
    public void updateStepRunning(String taskId, int step) {
        AsyncTask task = asyncTaskRepository.findByTaskId(taskId);
        if (task == null) return;

        List<AsyncTaskStepDetail> steps = JSONUtil.toList(task.getSteps(), AsyncTaskStepDetail.class);
        if (steps == null) {
            steps = Collections.emptyList();
        } else {
            steps = new ArrayList<>(steps);
            steps.sort(Comparator.comparingInt(AsyncTaskStepDetail::step));
        }
        // 小于当前步号的步骤视为已完成，当前步 running，其余 pending
        List<AsyncTaskStepDetail> updated = new ArrayList<>(steps.size());
        for (AsyncTaskStepDetail s : steps) {
            AsyncTaskStepStatus nextStatus;
            if (s.step() < step) {
                nextStatus = AsyncTaskStepStatus.COMPLETED;
            } else if (s.step() == step) {
                nextStatus = AsyncTaskStepStatus.RUNNING;
            } else {
                nextStatus = AsyncTaskStepStatus.PENDING;
            }
            updated.add(s.withStatus(nextStatus));
        }

        task.setSteps(JSONUtil.toJsonStr(updated));
        task.setStatus(AsyncTaskStatus.RUNNING);
        task.setCurrentStep(step);
        asyncTaskRepository.updateById(task);
    }

    /**
     * 标记某步骤为 running，并记录开始时间。
     * 与 updateStepRunning 不同，此方法精确更新指定步骤并保留其他字段。
     */
    public void startStep(String taskId, int step) {
        AsyncTask task = asyncTaskRepository.findByTaskId(taskId);
        if (task == null) return;

        List<AsyncTaskStepDetail> steps = parseSteps(task.getSteps());
        List<AsyncTaskStepDetail> updated = steps.stream()
                .map(s -> s.step() == step ? s.start() : s)
                .collect(java.util.stream.Collectors.toList());

        task.setSteps(JSONUtil.toJsonStr(updated));
        task.setStatus(AsyncTaskStatus.RUNNING);
        task.setCurrentStep(step);
        asyncTaskRepository.updateById(task);
    }

    /**
     * 标记某步骤为 completed，记录结束时间、耗时和摘要。
     */
    public void finishStep(String taskId, int step, java.util.Map<String, Object> summary) {
        AsyncTask task = asyncTaskRepository.findByTaskId(taskId);
        if (task == null) return;

        List<AsyncTaskStepDetail> steps = parseSteps(task.getSteps());
        List<AsyncTaskStepDetail> updated = steps.stream()
                .map(s -> s.step() == step ? s.complete(summary) : s)
                .collect(java.util.stream.Collectors.toList());

        task.setSteps(JSONUtil.toJsonStr(updated));
        asyncTaskRepository.updateById(task);
    }

    private List<AsyncTaskStepDetail> parseSteps(String stepsJson) {
        List<AsyncTaskStepDetail> steps = JSONUtil.toList(stepsJson, AsyncTaskStepDetail.class);
        return steps != null ? new ArrayList<>(steps) : new ArrayList<>();
    }

    /**
     * 标记任务完成：所有步骤置为 completed，写入最终结果
     */
    public void completeTask(String taskId, String resultJson) {
        AsyncTask task = asyncTaskRepository.findByTaskId(taskId);
        if (task == null) return;

        List<AsyncTaskStepDetail> steps = JSONUtil.toList(task.getSteps(), AsyncTaskStepDetail.class);
        if (steps != null && !steps.isEmpty()) {
            List<AsyncTaskStepDetail> updated = new ArrayList<>(steps.size());
            for (AsyncTaskStepDetail s : steps) {
                updated.add(s.withStatus(AsyncTaskStepStatus.COMPLETED));
            }
            task.setSteps(JSONUtil.toJsonStr(updated));
        }

        task.setStatus(AsyncTaskStatus.COMPLETED);
        task.setResult(resultJson);
        asyncTaskRepository.updateById(task);
    }

    /**
     * 标记任务失败
     */
    public void failTask(String taskId, String errorMessage) {
        AsyncTask task = asyncTaskRepository.findByTaskId(taskId);
        if (task == null) return;

        task.setStatus(AsyncTaskStatus.FAILED);
        task.setErrorMessage(errorMessage);
        asyncTaskRepository.updateById(task);
    }

    /**
     * 查询任务
     */
    public AsyncTask getTask(String taskId) {
        return asyncTaskRepository.findByTaskId(taskId);
    }
}
