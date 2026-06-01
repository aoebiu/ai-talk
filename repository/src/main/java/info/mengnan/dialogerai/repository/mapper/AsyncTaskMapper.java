package info.mengnan.dialogerai.repository.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import info.mengnan.dialogerai.repository.entity.AsyncTask;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AsyncTaskMapper extends BaseMapper<AsyncTask> {

    default AsyncTask findByTaskId(String taskId) {
        LambdaQueryWrapper<AsyncTask> qw = new LambdaQueryWrapper<AsyncTask>()
                .eq(AsyncTask::getTaskId, taskId);
        return selectOne(qw);
    }
}
