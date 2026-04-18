package com.netbridge.module.task.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netbridge.framework.security.util.UserContextHolder;
import com.netbridge.framework.web.api.ApiResponse;
import com.netbridge.framework.web.api.PageResult;
import com.netbridge.framework.web.exception.BusinessException;
import com.netbridge.framework.web.socket.StatusPushService;
import com.netbridge.module.log.api.annotation.OperationAudit;
import com.netbridge.module.task.api.dto.TaskDto;
import com.netbridge.module.task.api.dto.TaskPayloadDto;
import com.netbridge.module.task.api.dto.TaskResultDto;
import com.netbridge.module.task.api.request.TaskCreateRequest;
import com.netbridge.module.task.api.request.TaskQueryRequest;
import com.netbridge.module.task.api.request.TaskUpdateRequest;
import com.netbridge.module.task.entity.TaskEntity;
import com.netbridge.module.task.service.TaskService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/task")
public class TaskController {

    private static final int MAX_RETRY_COUNT = 3;
    private static final int DEFAULT_TASK_TIMEOUT_MINUTES = 10;

    private final TaskService taskService;
    private final ObjectMapper objectMapper;
    private final StatusPushService statusPushService;

    public TaskController(TaskService taskService, ObjectMapper objectMapper, StatusPushService statusPushService) {
        this.taskService = taskService;
        this.objectMapper = objectMapper;
        this.statusPushService = statusPushService;
    }

    @GetMapping("/list")
    public ApiResponse<PageResult<TaskDto>> list(TaskQueryRequest request) {
        LambdaQueryWrapper<TaskEntity> wrapper = new LambdaQueryWrapper<TaskEntity>()
                .eq(request.getServerId() != null, TaskEntity::getServerId, request.getServerId())
                .eq(request.getServiceId() != null, TaskEntity::getServiceId, request.getServiceId())
                .eq(request.getStatus() != null && !request.getStatus().isBlank(), TaskEntity::getStatus, request.getStatus())
                .eq(request.getType() != null && !request.getType().isBlank(), TaskEntity::getType, request.getType())
                .orderByDesc(TaskEntity::getId);
        Page<TaskEntity> page = taskService.page(new Page<>(request.getPageNo(), request.getPageSize()), wrapper);
        return ApiResponse.success(new PageResult<>(
                page.getCurrent(),
                page.getSize(),
                page.getTotal(),
                page.getRecords().stream().map(this::toDto).toList()
        ));
    }

    @GetMapping("/{id}")
    public ApiResponse<TaskDto> getById(@PathVariable Long id) {
        return ApiResponse.success(toDto(taskService.getById(id)));
    }

    @PostMapping("/create")
    @OperationAudit(
            action = "TASK_CREATE",
            resourceType = "task",
            resourceId = "#result.data.id",
            successDetail = "'created task ' + #request.type",
            failureDetail = "'failed to create task: ' + #errorMessage"
            ,recordOnSuccess = true
    )
    public ApiResponse<TaskDto> create(@RequestBody TaskCreateRequest request) {
        assertNoInProgressTask(request.getServiceId(), null);
        TaskEntity entity = new TaskEntity();
        entity.setServerId(request.getServerId());
        entity.setServiceId(request.getServiceId());
        entity.setAgentId(request.getAgentId());
        entity.setType(request.getType());
        entity.setPayload(request.getPayload());
        entity.setStatus(TaskDto.STATUS_PENDING);
        entity.setRetryCount(0);
        taskService.save(entity);
        statusPushService.publishTaskStatus(UserContextHolder.getUserId(), entity.getId(), entity.getServerId(), entity.getServiceId(), entity.getStatus(), "task created");
        return ApiResponse.success(toDto(entity));
    }

    @PutMapping("/update")
    @OperationAudit(
            action = "TASK_UPDATE",
            resourceType = "task",
            resourceId = "#request.id",
            successDetail = "'updated task #' + #request.id",
            failureDetail = "'failed to update task #' + #request.id + ': ' + #errorMessage"
            ,recordOnSuccess = true
    )
    public ApiResponse<TaskDto> update(@RequestBody TaskUpdateRequest request) {
        TaskEntity entity = taskService.getById(request.getId());
        if (entity == null) {
            throw BusinessException.notFound("task not found");
        }
        entity.setStatus(request.getStatus());
        entity.setResult(request.getResult());
        entity.setErrorMessage(request.getErrorMessage());
        if (request.getRetryCount() != null) {
            entity.setRetryCount(request.getRetryCount());
        }
        if (TaskDto.STATUS_RUNNING.equals(request.getStatus()) && entity.getStartedAt() == null) {
            entity.setStartedAt(LocalDateTime.now());
        }
        if (TaskDto.STATUS_SUCCESS.equals(request.getStatus())
                || TaskDto.STATUS_FAILED.equals(request.getStatus())
                || TaskDto.STATUS_CANCELLED.equals(request.getStatus())
                || TaskDto.STATUS_TIMED_OUT.equals(request.getStatus())) {
            entity.setCompletedAt(LocalDateTime.now());
        }
        taskService.updateById(entity);
        statusPushService.publishTaskStatus(UserContextHolder.getUserId(), entity.getId(), entity.getServerId(), entity.getServiceId(), entity.getStatus(), "task updated");
        return ApiResponse.success(toDto(entity));
    }

    @PostMapping("/{id}/retry")
    @OperationAudit(
            action = "TASK_RETRY",
            resourceType = "task",
            resourceId = "#id",
            successDetail = "'retried task #' + #id",
            failureDetail = "'failed to retry task #' + #id + ': ' + #errorMessage"
            ,recordOnSuccess = true
    )
    public ApiResponse<TaskDto> retry(@PathVariable Long id) {
        TaskEntity entity = taskService.getById(id);
        if (entity == null) {
            throw BusinessException.notFound("task not found");
        }
        if (!TaskDto.STATUS_FAILED.equalsIgnoreCase(entity.getStatus())
                && !TaskDto.STATUS_TIMED_OUT.equalsIgnoreCase(entity.getStatus())) {
            throw BusinessException.badRequest("only failed or timed out tasks can be retried");
        }
        int retryCount = entity.getRetryCount() == null ? 0 : entity.getRetryCount();
        if (retryCount >= MAX_RETRY_COUNT) {
            throw BusinessException.conflict("task retry limit exceeded");
        }
        assertNoInProgressTask(entity.getServiceId(), entity.getId());
        entity.setStatus(TaskDto.STATUS_PENDING);
        entity.setAgentId(null);
        entity.setResult(null);
        entity.setErrorMessage(null);
        entity.setStartedAt(null);
        entity.setCompletedAt(null);
        entity.setRetryCount(retryCount + 1);
        taskService.updateById(entity);
        statusPushService.publishTaskStatus(UserContextHolder.getUserId(), entity.getId(), entity.getServerId(), entity.getServiceId(), entity.getStatus(), "task retried");
        return ApiResponse.success(toDto(entity));
    }

    @PostMapping("/{id}/cancel")
    @OperationAudit(
            action = "TASK_CANCEL",
            resourceType = "task",
            resourceId = "#id",
            successDetail = "'cancelled task #' + #id",
            failureDetail = "'failed to cancel task #' + #id + ': ' + #errorMessage"
            ,recordOnSuccess = true
    )
    public ApiResponse<TaskDto> cancel(@PathVariable Long id) {
        TaskEntity entity = taskService.getById(id);
        if (entity == null) {
            throw BusinessException.notFound("task not found");
        }
        if (!TaskDto.STATUS_PENDING.equalsIgnoreCase(entity.getStatus())
                && !TaskDto.STATUS_RUNNING.equalsIgnoreCase(entity.getStatus())) {
            throw BusinessException.badRequest("only pending or running tasks can be cancelled");
        }
        entity = taskService.cancelTask(id);
        return ApiResponse.success(toDto(entity));
    }

    @PostMapping("/timeout/cleanup")
    public ApiResponse<Long> cleanupTimedOutTasks() {
        return ApiResponse.success(taskService.cleanupTimedOutTasks(DEFAULT_TASK_TIMEOUT_MINUTES));
    }

    @DeleteMapping("/{id}")
    @OperationAudit(
            action = "TASK_DELETE",
            resourceType = "task",
            resourceId = "#id",
            successDetail = "'deleted task #' + #id",
            failureDetail = "'failed to delete task #' + #id + ': ' + #errorMessage"
            ,recordOnSuccess = true
    )
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        return ApiResponse.success(taskService.removeById(id));
    }

    private TaskDto toDto(TaskEntity entity) {
        if (entity == null) {
            return null;
        }
        TaskDto dto = new TaskDto();
        BeanUtils.copyProperties(entity, dto);
        dto.setPayloadObj(fromJson(entity.getPayload(), TaskPayloadDto.class));
        dto.setResultObj(fromJson(entity.getResult(), TaskResultDto.class));
        return dto;
    }

    private <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw BusinessException.internal("failed to parse task json");
        }
    }

    private void assertNoInProgressTask(Long serviceId, Long excludeTaskId) {
        if (taskService.hasInProgressTask(serviceId, excludeTaskId)) {
            throw BusinessException.conflict("service already has an in-progress task");
        }
    }
}
