package info.mengnan.aitalk.server.service;

import info.mengnan.aitalk.common.util.JSONUtil;
import info.mengnan.aitalk.repository.enums.AsyncTaskStatus;
import info.mengnan.aitalk.repository.config.AsyncTaskStepDetail;
import info.mengnan.aitalk.repository.enums.AsyncTaskStepStatus;
import info.mengnan.aitalk.repository.enums.AsyncTaskType;
import info.mengnan.aitalk.repository.entity.AsyncTask;
import info.mengnan.aitalk.repository.entity.DocumentInfo;
import info.mengnan.aitalk.repository.enums.DocumentStatus;
import info.mengnan.aitalk.repository.entity.KnowledgeBase;
import info.mengnan.aitalk.repository.repo.AsyncTaskRepository;
import info.mengnan.aitalk.repository.repo.DocumentInfoRepository;
import info.mengnan.aitalk.repository.repo.KnowledgeBaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库维度构建任务：按库内文档状态聚合四步流水线进度。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseBuildService {

    private static final List<String> KB_BUILD_STEP_LABELS = List.of(
            "文档解析", "内容清洗", "文本分块", "文档向量化"
    );

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final DocumentInfoRepository documentInfoRepository;
    private final AsyncTaskRepository asyncTaskRepository;
    private final AsyncTaskService asyncTaskService;

    /**
     * 确保知识库存在 {@link AsyncTaskType#KB_BUILD} 任务，并写入 {@code knowledge_base.build_task_id}。
     */
    public String ensureBuildTask(Long kbId, Long memberId) {
        KnowledgeBase kb = knowledgeBaseRepository.findById(kbId);
        if (kb == null) {
            throw new IllegalArgumentException("知识库不存在: " + kbId);
        }

        if (StringUtils.hasText(kb.getBuildTaskId())) {
            AsyncTask existing = asyncTaskRepository.findByTaskId(kb.getBuildTaskId());
            if (existing != null) {
                return kb.getBuildTaskId();
            }
        }

        String taskId = asyncTaskService.createTask(memberId, AsyncTaskType.KB_BUILD, KB_BUILD_STEP_LABELS);
        kb.setBuildTaskId(taskId);
        knowledgeBaseRepository.updateById(kb);
        log.info("KB_BUILD task created: kbId={}, taskId={}", kbId, taskId);
        return taskId;
    }

    /**
     * 根据库内文档处理状态刷新四步构建进度。
     */
    public void refreshBuildProgress(Long kbId) {
        KnowledgeBase kb = knowledgeBaseRepository.findById(kbId);
        if (kb == null || !StringUtils.hasText(kb.getBuildTaskId())) {
            return;
        }

        AsyncTask task = asyncTaskRepository.findByTaskId(kb.getBuildTaskId());
        if (task == null) {
            return;
        }

        List<DocumentInfo> docs = documentInfoRepository.findByKbId(kbId).stream()
                .filter(d -> d.getDeleted() == null || d.getDeleted() == 0)
                .toList();

        if (docs.isEmpty()) {
            return;
        }

        List<AsyncTaskStepDetail> previous = parseSteps(task.getSteps());
        List<AsyncTaskStepDetail> steps = buildAggregatedSteps(docs, previous);

        task.setSteps(JSONUtil.toJsonStr(steps));
        task.setTotalSteps(KB_BUILD_STEP_LABELS.size());
        task.setCurrentStep(resolveCurrentStep(steps));
        task.setStatus(resolveTaskStatus(docs, steps));
        if (task.getStatus() == AsyncTaskStatus.FAILED) {
            task.setErrorMessage(resolveFailureMessage(docs));
        } else if (task.getStatus() == AsyncTaskStatus.COMPLETED) {
            task.setErrorMessage(null);
            task.setResult(JSONUtil.toJsonStr(Map.of(
                    "kbId", kbId,
                    "documentCount", docs.size(),
                    "message", "知识库构建完成"
            )));
        }
        asyncTaskRepository.updateById(task);
    }

    private List<AsyncTaskStepDetail> buildAggregatedSteps(List<DocumentInfo> docs,
                                                           List<AsyncTaskStepDetail> previous) {
        List<AsyncTaskStepDetail> result = new ArrayList<>(KB_BUILD_STEP_LABELS.size());
        for (int i = 0; i < KB_BUILD_STEP_LABELS.size(); i++) {
            int stepNo = i + 1;
            String label = KB_BUILD_STEP_LABELS.get(i);
            AsyncTaskStepStatus status = resolveKbStepStatus(stepNo, docs);
            Map<String, Object> summary = buildStepSummary(stepNo, status, docs);
            AsyncTaskStepDetail prev = findStep(previous, stepNo);

            AsyncTaskStepDetail step = mergeStepTransition(prev, stepNo, label, status, summary);
            result.add(step);
        }
        return result;
    }

    private AsyncTaskStepDetail mergeStepTransition(AsyncTaskStepDetail prev,
                                                    int stepNo,
                                                    String label,
                                                    AsyncTaskStepStatus status,
                                                    Map<String, Object> summary) {
        if (prev == null) {
            prev = AsyncTaskStepDetail.pending(stepNo, label);
        }

        if (status == AsyncTaskStepStatus.RUNNING && prev.status() != AsyncTaskStepStatus.RUNNING) {
            return prev.start();
        }
        if (status == AsyncTaskStepStatus.COMPLETED && prev.status() != AsyncTaskStepStatus.COMPLETED) {
            return prev.complete(summary);
        }
        if (status == AsyncTaskStepStatus.COMPLETED && prev.status() == AsyncTaskStepStatus.COMPLETED) {
            return new AsyncTaskStepDetail(
                    stepNo, label, AsyncTaskStepStatus.COMPLETED,
                    prev.startedAt(), prev.finishedAt(), prev.durationMs(), summary);
        }
        if (status == AsyncTaskStepStatus.RUNNING) {
            return new AsyncTaskStepDetail(
                    stepNo, label, AsyncTaskStepStatus.RUNNING,
                    prev.startedAt() != null ? prev.startedAt() : prev.start().startedAt(),
                    null, null, summary);
        }
        return new AsyncTaskStepDetail(stepNo, label, status, prev.startedAt(), prev.finishedAt(), prev.durationMs(), summary);
    }

    private AsyncTaskStepStatus resolveKbStepStatus(int step, List<DocumentInfo> docs) {
        if (docs.stream().allMatch(d -> DocumentStatus.DONE.name().equals(d.getStatus()))) {
            return AsyncTaskStepStatus.COMPLETED;
        }
        if (docs.stream().allMatch(d -> DocumentStatus.FAILED.name().equals(d.getStatus()))) {
            return step == KB_BUILD_STEP_LABELS.size()
                    ? AsyncTaskStepStatus.COMPLETED
                    : AsyncTaskStepStatus.PENDING;
        }

        boolean completed = docs.stream().allMatch(d -> docPhase(d) > step || DocumentStatus.FAILED.name().equals(d.getStatus()));
        if (completed) {
            return AsyncTaskStepStatus.COMPLETED;
        }

        boolean running = docs.stream().anyMatch(d -> {
            int phase = docPhase(d);
            return phase == step || (step == 1 && phase == 0);
        });
        if (running) {
            return AsyncTaskStepStatus.RUNNING;
        }
        return AsyncTaskStepStatus.PENDING;
    }

    /**
     * 文档所处流水线阶段：0=待处理, 1~4=四步处理中, 5=已完成。
     */
    private int docPhase(DocumentInfo doc) {
        String status = doc.getStatus();
        if (status == null) {
            return 0;
        }
        return switch (DocumentStatus.valueOf(status)) {
            case PENDING -> 0;
            case PARSING -> 1;
            case CLEANING -> 2;
            case CHUNKING -> 3;
            case EMBEDDING -> 4;
            case DONE -> 5;
            case FAILED -> 5;
        };
    }

    private Map<String, Object> buildStepSummary(int step, AsyncTaskStepStatus status, List<DocumentInfo> docs) {
        Map<String, Object> summary = new HashMap<>();
        String message = buildStepMessage(step, status, docs);
        if (StringUtils.hasText(message)) {
            summary.put("message", message);
        }
        if (step == 4 && status == AsyncTaskStepStatus.RUNNING) {
            DocumentInfo active = findActiveDocForStep(step, docs);
            if (active != null) {
                int processed = active.getProcessedChunks() != null ? active.getProcessedChunks() : 0;
                int total = active.getTotalChunks() != null ? active.getTotalChunks() : 0;
                summary.put("processedChunks", processed);
                summary.put("totalChunks", total);
            }
        }
        long doneCount = docs.stream().filter(d -> DocumentStatus.DONE.name().equals(d.getStatus())).count();
        summary.put("doneDocuments", doneCount);
        summary.put("totalDocuments", docs.size());
        return summary.isEmpty() ? null : summary;
    }

    private String buildStepMessage(int step, AsyncTaskStepStatus status, List<DocumentInfo> docs) {
        if (status == AsyncTaskStepStatus.COMPLETED) {
            return switch (step) {
                case 1 -> "文档解析完成";
                case 2 -> "内容清洗完成";
                case 3 -> "文本分块完成";
                case 4 -> "文档向量化完成";
                default -> null;
            };
        }
        if (status != AsyncTaskStepStatus.RUNNING) {
            return null;
        }
        DocumentInfo active = findActiveDocForStep(step, docs);
        if (active == null) {
            long pending = docs.stream().filter(d -> DocumentStatus.PENDING.name().equals(d.getStatus())).count();
            if (step == 1 && pending > 0) {
                return pending + " 个文档等待解析";
            }
            return null;
        }
        String name = active.getOriginalName();
        return switch (step) {
            case 1 -> name + " 正在解析";
            case 2 -> name + " 正在清洗";
            case 3 -> name + " 正在分块";
            case 4 -> {
                int processed = active.getProcessedChunks() != null ? active.getProcessedChunks() : 0;
                int total = active.getTotalChunks() != null ? active.getTotalChunks() : 0;
                yield name + " 正在向量化（" + processed + "/" + total + "）";
            }
            default -> null;
        };
    }

    private DocumentInfo findActiveDocForStep(int step, List<DocumentInfo> docs) {
        DocumentStatus target = switch (step) {
            case 1 -> DocumentStatus.PARSING;
            case 2 -> DocumentStatus.CLEANING;
            case 3 -> DocumentStatus.CHUNKING;
            case 4 -> DocumentStatus.EMBEDDING;
            default -> null;
        };
        if (target != null) {
            return docs.stream()
                    .filter(d -> target.name().equals(d.getStatus()))
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    private int resolveCurrentStep(List<AsyncTaskStepDetail> steps) {
        return steps.stream()
                .filter(s -> s.status() != AsyncTaskStepStatus.COMPLETED)
                .map(AsyncTaskStepDetail::step)
                .min(Comparator.naturalOrder())
                .orElse(KB_BUILD_STEP_LABELS.size());
    }

    private AsyncTaskStatus resolveTaskStatus(List<DocumentInfo> docs, List<AsyncTaskStepDetail> steps) {
        boolean anyFailed = docs.stream().anyMatch(d -> DocumentStatus.FAILED.name().equals(d.getStatus()));
        boolean allDone = docs.stream().allMatch(d -> DocumentStatus.DONE.name().equals(d.getStatus()));
        boolean allTerminal = docs.stream().allMatch(d -> {
            String s = d.getStatus();
            return DocumentStatus.DONE.name().equals(s) || DocumentStatus.FAILED.name().equals(s);
        });

        if (allDone) {
            return AsyncTaskStatus.COMPLETED;
        }
        if (allTerminal && anyFailed) {
            return AsyncTaskStatus.FAILED;
        }
        boolean anyRunning = steps.stream().anyMatch(s -> s.status() == AsyncTaskStepStatus.RUNNING);
        if (anyRunning || docs.stream().anyMatch(d -> !DocumentStatus.DONE.name().equals(d.getStatus())
                && !DocumentStatus.FAILED.name().equals(d.getStatus()))) {
            return AsyncTaskStatus.RUNNING;
        }
        return AsyncTaskStatus.PENDING;
    }

    private String resolveFailureMessage(List<DocumentInfo> docs) {
        return docs.stream()
                .filter(d -> DocumentStatus.FAILED.name().equals(d.getStatus()))
                .map(d -> d.getOriginalName() + ": " + (d.getErrorMessage() != null ? d.getErrorMessage() : "处理失败"))
                .findFirst()
                .orElse("部分文档处理失败");
    }

    private AsyncTaskStepDetail findStep(List<AsyncTaskStepDetail> steps, int stepNo) {
        if (steps == null) {
            return null;
        }
        return steps.stream().filter(s -> s.step() == stepNo).findFirst().orElse(null);
    }

    private List<AsyncTaskStepDetail> parseSteps(String stepsJson) {
        List<AsyncTaskStepDetail> steps = JSONUtil.toList(stepsJson, AsyncTaskStepDetail.class);
        if (steps == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(steps);
    }
}
