package com.netbridge.module.task.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.netbridge.framework.web.socket.StatusPushService;
import com.netbridge.module.task.api.dto.TaskDto;
import com.netbridge.module.task.entity.TaskEntity;
import com.netbridge.module.task.mapper.TaskLifecycleMapper;
import com.netbridge.module.task.mapper.TaskMapper;
import com.netbridge.module.task.service.TaskService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TaskServiceImpl extends ServiceImpl<TaskMapper, TaskEntity> implements TaskService {

    private final TaskLifecycleMapper taskLifecycleMapper;
    private final StatusPushService statusPushService;

    public TaskServiceImpl(TaskLifecycleMapper taskLifecycleMapper, StatusPushService statusPushService) {
        this.taskLifecycleMapper = taskLifecycleMapper;
        this.statusPushService = statusPushService;
    }

    @Override
    public boolean hasInProgressTask(Long serviceId, Long excludeTaskId) {
        if (serviceId == null) {
            return false;
        }
        Long count = lambdaQuery()
                .eq(TaskEntity::getServiceId, serviceId)
                .ne(excludeTaskId != null, TaskEntity::getId, excludeTaskId)
                .in(TaskEntity::getStatus, TaskDto.STATUS_PENDING, TaskDto.STATUS_RUNNING)
                .count();
        return count != null && count > 0;
    }

    @Override
    public long cleanupTimedOutTasks(int timeoutMinutes) {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(timeoutMinutes);
        List<TaskEntity> timedOutTasks = lambdaQuery()
                .eq(TaskEntity::getStatus, TaskDto.STATUS_RUNNING)
                .isNotNull(TaskEntity::getStartedAt)
                .lt(TaskEntity::getStartedAt, threshold)
                .list();
        if (timedOutTasks.isEmpty()) {
            return 0L;
        }
        LocalDateTime completedAt = LocalDateTime.now();
        timedOutTasks.forEach(task -> {
            task.setStatus(TaskDto.STATUS_TIMED_OUT);
            task.setErrorMessage("task timed out after " + timeoutMinutes + " minutes");
            task.setCompletedAt(completedAt);
        });
        updateBatchById(timedOutTasks);
        timedOutTasks.forEach(task -> {
            syncServiceAfterClosedTask(task, TaskDto.STATUS_TIMED_OUT);
            statusPushService.publishTaskStatus(resolveUserId(task.getServerId()), task.getId(), task.getServerId(), task.getServiceId(), task.getStatus(), task.getErrorMessage());
        });
        return timedOutTasks.size();
    }

    @Override
    public TaskEntity cancelTask(Long taskId) {
        TaskEntity entity = getById(taskId);
        if (entity == null) {
            return null;
        }
        entity.setStatus(TaskDto.STATUS_CANCELLED);
        entity.setErrorMessage("task cancelled manually");
        entity.setCompletedAt(LocalDateTime.now());
        updateById(entity);
        syncServiceAfterClosedTask(entity, TaskDto.STATUS_CANCELLED);
        statusPushService.publishTaskStatus(resolveUserId(entity.getServerId()), entity.getId(), entity.getServerId(), entity.getServiceId(), entity.getStatus(), entity.getErrorMessage());
        return entity;
    }

    private void syncServiceAfterClosedTask(TaskEntity taskEntity, String closedStatus) {
        if (taskEntity.getServiceId() == null) {
            return;
        }
        if ("CHECK".equalsIgnoreCase(taskEntity.getType())) {
            String healthStatus = TaskDto.STATUS_TIMED_OUT.equalsIgnoreCase(closedStatus) ? "error" : "unknown";
            taskLifecycleMapper.updateServiceHealth(taskEntity.getServiceId(), healthStatus, LocalDateTime.now());
            statusPushService.publishServiceStatus(resolveUserId(taskEntity.getServerId()), taskEntity.getServiceId(), taskEntity.getServerId(), null, healthStatus, "service health updated by closed task");
            return;
        }
        if (TaskDto.STATUS_TIMED_OUT.equalsIgnoreCase(closedStatus)) {
            taskLifecycleMapper.updateServiceStatusAndHealth(taskEntity.getServiceId(), "error", "error");
            statusPushService.publishServiceStatus(resolveUserId(taskEntity.getServerId()), taskEntity.getServiceId(), taskEntity.getServerId(), "error", "error", "service marked error after task timeout");
            return;
        }
        if (TaskDto.STATUS_CANCELLED.equalsIgnoreCase(closedStatus)) {
            taskLifecycleMapper.updateServiceStatusAndHealth(taskEntity.getServiceId(), "error", "warning");
            statusPushService.publishServiceStatus(resolveUserId(taskEntity.getServerId()), taskEntity.getServiceId(), taskEntity.getServerId(), "error", "warning", "service marked warning after task cancellation");
        }
    }

    private Long resolveUserId(Long serverId) {
        if (serverId == null) {
            return null;
        }
        return taskLifecycleMapper.findUserIdByServerId(serverId);
    }
}
