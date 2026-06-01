package info.mengnan.dialogerai.repository.repo;

import info.mengnan.dialogerai.repository.entity.AsyncTask;
import info.mengnan.dialogerai.repository.mapper.AsyncTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AsyncTaskRepository {

    private final AsyncTaskMapper mapper;

    public void insert(AsyncTask entity) {
        mapper.insert(entity);
    }

    public void updateById(AsyncTask entity) {
        mapper.updateById(entity);
    }

    public AsyncTask findByTaskId(String taskId) {
        return mapper.findByTaskId(taskId);
    }
}
