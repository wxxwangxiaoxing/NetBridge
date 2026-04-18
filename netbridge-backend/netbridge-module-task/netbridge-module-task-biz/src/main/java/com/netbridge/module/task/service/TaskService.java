package com.netbridge.module.task.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.netbridge.module.task.entity.TaskEntity;

public interface TaskService extends IService<TaskEntity> {

    boolean hasInProgressTask(Long serviceId, Long excludeTaskId);

    long cleanupTimedOutTasks(int timeoutMinutes);

    TaskEntity cancelTask(Long taskId);
}
