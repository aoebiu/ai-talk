package info.mengnan.aitalk.repository.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import info.mengnan.aitalk.repository.async.AsyncTaskStatus;
import info.mengnan.aitalk.repository.async.AsyncTaskType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("async_task")
public class AsyncTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskId;

    private Long memberId;

    private AsyncTaskType taskType;

    private AsyncTaskStatus status;

    private Integer currentStep;

    private Integer totalSteps;

    /**
     * 步骤详情 JSON 数组，结构见 {@link info.mengnan.aitalk.repository.async.AsyncTaskStepDetail}
     */
    private String steps;

    private String result;

    private String errorMessage;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
