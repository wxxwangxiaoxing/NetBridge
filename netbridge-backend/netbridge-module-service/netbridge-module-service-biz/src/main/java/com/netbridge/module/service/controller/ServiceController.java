package com.netbridge.module.service.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.netbridge.framework.security.util.UserContextHolder;
import com.netbridge.framework.web.api.ApiResponse;
import com.netbridge.framework.web.api.PageResult;
import com.netbridge.framework.web.exception.BusinessException;
import com.netbridge.module.service.api.dto.ServiceActionResponse;
import com.netbridge.module.service.api.dto.ServiceDetailDto;
import com.netbridge.module.service.api.dto.ServiceDto;
import com.netbridge.module.task.api.dto.TaskPayloadDto;
import com.netbridge.module.log.api.dto.LogDto;
import com.netbridge.module.log.entity.LogEntity;
import com.netbridge.module.log.service.LogService;
import com.netbridge.module.monitor.api.dto.ServiceMetricDto;
import com.netbridge.module.monitor.entity.ServiceMetricEntity;
import com.netbridge.module.monitor.service.ServiceMetricService;
import com.netbridge.module.service.api.request.ServiceCreateRequest;
import com.netbridge.module.service.api.request.ServiceQueryRequest;
import com.netbridge.module.service.api.request.ServiceUpdateRequest;
import com.netbridge.module.service.entity.ServiceEntity;
import com.netbridge.module.service.service.ServiceBizService;
import com.netbridge.module.task.api.dto.TaskDto;
import com.netbridge.module.task.entity.TaskEntity;
import com.netbridge.module.task.service.TaskService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netbridge.framework.web.socket.StatusPushService;
import com.netbridge.module.log.api.annotation.OperationAudit;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/api/service")
public class ServiceController {

    private final ServiceBizService serviceBizService;
    private final TaskService taskService;
    private final LogService logService;
    private final ServiceMetricService serviceMetricService;
    private final ObjectMapper objectMapper;
    private final StatusPushService statusPushService;

    public ServiceController(
            ServiceBizService serviceBizService,
            TaskService taskService,
            LogService logService,
            ServiceMetricService serviceMetricService,
            ObjectMapper objectMapper,
            StatusPushService statusPushService
    ) {
        this.serviceBizService = serviceBizService;
        this.taskService = taskService;
        this.logService = logService;
        this.serviceMetricService = serviceMetricService;
        this.objectMapper = objectMapper;
        this.statusPushService = statusPushService;
    }

    @GetMapping("/list")
    public ApiResponse<PageResult<ServiceDto>> list(ServiceQueryRequest request) {
        LambdaQueryWrapper<ServiceEntity> wrapper = new LambdaQueryWrapper<ServiceEntity>()
                .eq(request.getServerId() != null, ServiceEntity::getServerId, request.getServerId())
                .eq(request.getType() != null && !request.getType().isBlank(), ServiceEntity::getType, request.getType())
                .eq(request.getStatus() != null && !request.getStatus().isBlank(), ServiceEntity::getStatus, request.getStatus())
                .orderByDesc(ServiceEntity::getId);
        Page<ServiceEntity> page = serviceBizService.page(new Page<>(request.getPageNo(), request.getPageSize()), wrapper);
        return ApiResponse.success(new PageResult<>(
                page.getCurrent(),
                page.getSize(),
                page.getTotal(),
                page.getRecords().stream().map(this::toDto).toList()
        ));
    }

    @GetMapping("/{id}")
    public ApiResponse<ServiceDto> getById(@PathVariable Long id) {
        return ApiResponse.success(toDto(serviceBizService.getById(id)));
    }

    @GetMapping("/{id}/detail")
    public ApiResponse<ServiceDetailDto> detail(@PathVariable Long id) {
        ServiceEntity entity = serviceBizService.getById(id);
        if (entity == null) {
            throw BusinessException.notFound("service not found");
        }
        ServiceDetailDto dto = new ServiceDetailDto();
        BeanUtils.copyProperties(entity, dto);
        dto.setRecentTaskTotal(taskService.lambdaQuery().eq(TaskEntity::getServiceId, id).count());
        dto.setRecentTasks(taskService.lambdaQuery()
                .eq(TaskEntity::getServiceId, id)
                .orderByDesc(TaskEntity::getId)
                .last("limit 10")
                .list()
                .stream()
                .map(this::toTaskDto)
                .toList());
        dto.setRecentLogTotal(logService.lambdaQuery().eq(LogEntity::getServiceId, id).count());
        dto.setRecentLogs(logService.lambdaQuery()
                .eq(LogEntity::getServiceId, id)
                .orderByDesc(LogEntity::getId)
                .last("limit 20")
                .list()
                .stream()
                .map(this::toLogDto)
                .toList());
        dto.setMetrics(serviceMetricService.lambdaQuery()
                .eq(ServiceMetricEntity::getServiceId, id)
                .orderByDesc(ServiceMetricEntity::getCollectedAt)
                .last("limit 20")
                .list()
                .stream()
                .map(this::toMetricDto)
                .toList());
        return ApiResponse.success(dto);
    }

    @PostMapping("/create")
    @OperationAudit(
            action = "SERVICE_CREATE",
            resourceType = "service",
            resourceId = "#result.data.id",
            successDetail = "'created service ' + #request.name",
            failureDetail = "'failed to create service ' + #request.name + ': ' + #errorMessage"
            ,recordOnSuccess = true
    )
    public ApiResponse<ServiceDto> create(@RequestBody ServiceCreateRequest request) {
        ServiceEntity entity = new ServiceEntity();
        entity.setServerId(request.getServerId());
        entity.setName(request.getName());
        entity.setType(request.getType());
        entity.setProtocol(request.getProtocol());
        entity.setPort(request.getPort());
        entity.setAccessType(request.getAccessType());
        entity.setDomain(request.getDomain());
        entity.setEnabled(0);
        entity.setStatus("inactive");
        entity.setHealthStatus("unknown");
        entity.setTargetHost("127.0.0.1");
        serviceBizService.save(entity);
        statusPushService.publishServiceStatus(UserContextHolder.getUserId(), entity.getId(), entity.getServerId(), entity.getStatus(), entity.getHealthStatus(), "service created");
        return ApiResponse.success(toDto(entity));
    }

    @PutMapping("/update")
    @OperationAudit(
            action = "SERVICE_UPDATE",
            resourceType = "service",
            resourceId = "#request.id",
            successDetail = "'updated service #' + #request.id",
            failureDetail = "'failed to update service #' + #request.id + ': ' + #errorMessage"
            ,recordOnSuccess = true
    )
    public ApiResponse<ServiceDto> update(@RequestBody ServiceUpdateRequest request) {
        ServiceEntity entity = serviceBizService.getById(request.getId());
        if (entity == null) {
            throw BusinessException.notFound("service not found");
        }
        if (taskService.hasInProgressTask(entity.getId(), null)) {
            throw BusinessException.conflict("service has an in-progress task and cannot be updated");
        }
        entity.setName(request.getName());
        entity.setType(request.getType());
        entity.setProtocol(request.getProtocol());
        entity.setPort(request.getPort());
        entity.setTargetHost(request.getTargetHost());
        entity.setDomain(request.getDomain());
        entity.setEnabled(request.getEnabled());
        entity.setStatus(request.getStatus());
        entity.setAccessType(request.getAccessType());
        entity.setAccessUrl(request.getAccessUrl());
        entity.setHealthStatus(request.getHealthStatus());
        entity.setRemark(request.getRemark());
        serviceBizService.updateById(entity);
        statusPushService.publishServiceStatus(UserContextHolder.getUserId(), entity.getId(), entity.getServerId(), entity.getStatus(), entity.getHealthStatus(), "service updated");
        return ApiResponse.success(toDto(entity));
    }

    @DeleteMapping("/{id}")
    @OperationAudit(
            action = "SERVICE_DELETE",
            resourceType = "service",
            resourceId = "#id",
            successDetail = "'deleted service #' + #id",
            failureDetail = "'failed to delete service #' + #id + ': ' + #errorMessage"
            ,recordOnSuccess = true
    )
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        ServiceEntity entity = serviceBizService.getById(id);
        if (entity == null) {
            throw BusinessException.notFound("service not found");
        }
        assertNoInProgressTask(entity.getId());
        if (entity.getEnabled() != null && entity.getEnabled() == 1) {
            throw BusinessException.conflict("service is active, stop it before deleting");
        }
        if ("active".equalsIgnoreCase(entity.getStatus())) {
            throw BusinessException.conflict("service is active, stop it before deleting");
        }
        boolean removed = serviceBizService.removeById(id);
        if (removed) {
            statusPushService.publishServiceStatus(UserContextHolder.getUserId(), id, entity.getServerId(), "deleted", entity.getHealthStatus(), "service deleted");
        }
        return ApiResponse.success(removed);
    }

    @PostMapping("/{id}/start")
    @OperationAudit(
            action = "SERVICE_START",
            resourceType = "service",
            resourceId = "#result.data.taskId",
            successDetail = "'created START task for service #' + #id",
            failureDetail = "'failed to start service #' + #id + ': ' + #errorMessage"
            ,recordOnSuccess = true
    )
    public ApiResponse<ServiceActionResponse> start(@PathVariable Long id) {
        return ApiResponse.success(createTask(id, "START"));
    }

    @PostMapping("/{id}/stop")
    @OperationAudit(
            action = "SERVICE_STOP",
            resourceType = "service",
            resourceId = "#result.data.taskId",
            successDetail = "'created STOP task for service #' + #id",
            failureDetail = "'failed to stop service #' + #id + ': ' + #errorMessage"
            ,recordOnSuccess = true
    )
    public ApiResponse<ServiceActionResponse> stop(@PathVariable Long id) {
        return ApiResponse.success(createTask(id, "STOP"));
    }

    @PostMapping("/{id}/restart")
    @OperationAudit(
            action = "SERVICE_RESTART",
            resourceType = "service",
            resourceId = "#result.data.taskId",
            successDetail = "'created RESTART task for service #' + #id",
            failureDetail = "'failed to restart service #' + #id + ': ' + #errorMessage"
            ,recordOnSuccess = true
    )
    public ApiResponse<ServiceActionResponse> restart(@PathVariable Long id) {
        return ApiResponse.success(createTask(id, "RESTART"));
    }

    @GetMapping("/{id}/check")
    @OperationAudit(
            action = "SERVICE_CHECK",
            resourceType = "service",
            resourceId = "#result.data.taskId",
            successDetail = "'created CHECK task for service #' + #id",
            failureDetail = "'failed to check service #' + #id + ': ' + #errorMessage"
            ,recordOnSuccess = true
    )
    public ApiResponse<ServiceActionResponse> check(@PathVariable Long id) {
        return ApiResponse.success(createTask(id, "CHECK"));
    }

    private ServiceActionResponse createTask(Long serviceId, String type) {
        ServiceEntity serviceEntity = serviceBizService.getById(serviceId);
        if (serviceEntity == null) {
            throw BusinessException.notFound("service not found");
        }
        assertNoInProgressTask(serviceId);
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setServerId(serviceEntity.getServerId());
        taskEntity.setServiceId(serviceEntity.getId());
        taskEntity.setType(type);
        taskEntity.setStatus(TaskDto.STATUS_PENDING);
        taskEntity.setRetryCount(0);
        TaskPayloadDto payloadDto = new TaskPayloadDto();
        payloadDto.setServiceId(serviceEntity.getId());
        payloadDto.setServerId(serviceEntity.getServerId());
        payloadDto.setAction(type);
        payloadDto.setServiceName(serviceEntity.getName());
        payloadDto.setPort(serviceEntity.getPort());
        payloadDto.setProtocol(serviceEntity.getProtocol());
        payloadDto.setAccessType(serviceEntity.getAccessType());
        payloadDto.setTargetHost(serviceEntity.getTargetHost());
        payloadDto.setDomain(serviceEntity.getDomain());
        taskEntity.setPayload(toJson(payloadDto));
        taskService.save(taskEntity);
        statusPushService.publishTaskStatus(UserContextHolder.getUserId(), taskEntity.getId(), taskEntity.getServerId(), taskEntity.getServiceId(), taskEntity.getStatus(), "task created");
        return new ServiceActionResponse(taskEntity.getId(), taskEntity.getType(), taskEntity.getStatus());
    }

    private void assertNoInProgressTask(Long serviceId) {
        if (taskService.hasInProgressTask(serviceId, null)) {
            throw BusinessException.conflict("service already has an in-progress task");
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw BusinessException.internal("failed to serialize task payload");
        }
    }

    private ServiceDto toDto(ServiceEntity entity) {
        if (entity == null) {
            return null;
        }
        ServiceDto dto = new ServiceDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    private TaskDto toTaskDto(TaskEntity entity) {
        TaskDto dto = new TaskDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    private LogDto toLogDto(LogEntity entity) {
        LogDto dto = new LogDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    private ServiceMetricDto toMetricDto(ServiceMetricEntity entity) {
        ServiceMetricDto dto = new ServiceMetricDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }
}
