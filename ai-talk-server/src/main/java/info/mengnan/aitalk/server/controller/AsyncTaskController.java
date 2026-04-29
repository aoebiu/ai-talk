package info.mengnan.aitalk.server.controller;

import info.mengnan.aitalk.repository.entity.AsyncTask;
import info.mengnan.aitalk.server.param.R;
import info.mengnan.aitalk.server.service.AsyncTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/task")
@RequiredArgsConstructor
public class AsyncTaskController {

    private final AsyncTaskService asyncTaskService;

    @GetMapping("/{taskId}")
    public R getTask(@PathVariable("taskId") String taskId) {
        AsyncTask task = asyncTaskService.getTask(taskId);
        if (task == null) {
            return R.error("任务不存在");
        }
        return R.ok(task);
    }
}
