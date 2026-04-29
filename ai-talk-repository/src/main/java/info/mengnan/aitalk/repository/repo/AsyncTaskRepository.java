package info.mengnan.aitalk.repository.repo;

import info.mengnan.aitalk.repository.entity.AsyncTask;
import info.mengnan.aitalk.repository.mapper.AsyncTaskMapper;
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
