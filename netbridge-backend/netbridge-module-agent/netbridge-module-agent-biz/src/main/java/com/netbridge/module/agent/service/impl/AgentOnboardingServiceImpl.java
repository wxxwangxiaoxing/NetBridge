package com.netbridge.module.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netbridge.framework.web.socket.StatusPushService;
import com.netbridge.module.agent.api.dto.AgentTaskResponse;
import com.netbridge.framework.web.exception.BusinessException;
import com.netbridge.module.agent.api.dto.AgentRegisterResponse;
import com.netbridge.module.agent.api.request.AgentHeartbeatRequest;
import com.netbridge.module.agent.api.request.AgentLogRequest;
import com.netbridge.module.agent.api.request.AgentRegisterRequest;
import com.netbridge.module.agent.api.request.AgentTaskPullRequest;
import com.netbridge.module.agent.api.request.AgentTaskResultRequest;
import com.netbridge.module.agent.service.AgentOnboardingService;
import com.netbridge.module.monitor.entity.ServerMetricEntity;
import com.netbridge.module.monitor.entity.ServiceMetricEntity;
import com.netbridge.module.monitor.service.ServerMetricService;
import com.netbridge.module.monitor.service.ServiceMetricService;
import com.netbridge.module.service.entity.ServiceEntity;
import com.netbridge.module.service.service.ServiceBizService;
import com.netbridge.module.server.entity.InstallTokenEntity;
import com.netbridge.module.server.entity.ServerEntity;
import com.netbridge.module.server.service.InstallTokenService;
import com.netbridge.module.server.service.ServerService;
import com.netbridge.module.task.api.dto.TaskDto;
import com.netbridge.module.task.entity.TaskEntity;
import com.netbridge.module.task.api.dto.TaskPayloadDto;
import com.netbridge.module.task.api.dto.TaskResultDto;
import com.netbridge.module.task.service.TaskService;
import com.netbridge.module.log.entity.LogEntity;
import com.netbridge.module.log.service.LogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AgentOnboardingServiceImpl implements AgentOnboardingService {

    private final InstallTokenService installTokenService;
    private final ServerService serverService;
    private final TaskService taskService;
    private final LogService logService;
    private final ServiceBizService serviceBizService;
    private final ServerMetricService serverMetricService;
    private final ServiceMetricService serviceMetricService;
    private final ObjectMapper objectMapper;
    private final StatusPushService statusPushService;

    public AgentOnboardingServiceImpl(
            InstallTokenService installTokenService,
            ServerService serverService,
            TaskService taskService,
            LogService logService,
            ServiceBizService serviceBizService,
            ServerMetricService serverMetricService,
            ServiceMetricService serviceMetricService,
            ObjectMapper objectMapper,
            StatusPushService statusPushService
    ) {
        this.installTokenService = installTokenService;
        this.serverService = serverService;
        this.taskService = taskService;
        this.logService = logService;
        this.serviceBizService = serviceBizService;
        this.serverMetricService = serverMetricService;
        this.serviceMetricService = serviceMetricService;
        this.objectMapper = objectMapper;
        this.statusPushService = statusPushService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentRegisterResponse register(AgentRegisterRequest request) {
        installTokenService.expireUnusedTokens();
        InstallTokenEntity tokenEntity = installTokenService.lambdaQuery()
                .eq(InstallTokenEntity::getToken, request.getToken())
                .one();
        if (tokenEntity == null) {
            throw BusinessException.notFound("install token not found");
        }
        if ("expired".equals(tokenEntity.getStatus())) {
            throw BusinessException.conflict("install token expired");
        }
        if (tokenEntity.getExpiresAt() != null && tokenEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            tokenEntity.setStatus("expired");
            installTokenService.updateById(tokenEntity);
            throw BusinessException.conflict("install token expired");
        }
        String hostname = firstNonBlank(request.getHostname(), tokenEntity.getHostname());
        if (hostname == null) {
            throw BusinessException.badRequest("hostname is required");
        }
        ServerEntity serverEntity = findServerByHostname(tokenEntity.getUserId(), hostname);

        if ("used".equals(tokenEntity.getStatus())) {
            if (serverEntity == null) {
                throw BusinessException.conflict("install token already used");
            }
            fillServerRegistrationFields(serverEntity, tokenEntity, request, hostname);
            serverService.updateById(serverEntity);
            return toRegisterResponse(serverEntity);
        }
        if (!"unused".equals(tokenEntity.getStatus())) {
            throw BusinessException.conflict("install token already used or invalid");
        }

        if (serverEntity == null) {
            serverEntity = new ServerEntity();
            serverEntity.setUserId(tokenEntity.getUserId());
            serverEntity.setAgentId(generateAgentId());
            fillServerRegistrationFields(serverEntity, tokenEntity, request, hostname);
            serverService.save(serverEntity);
        } else {
            if (serverEntity.getAgentId() == null || serverEntity.getAgentId().isBlank()) {
                serverEntity.setAgentId(generateAgentId());
            }
            fillServerRegistrationFields(serverEntity, tokenEntity, request, hostname);
            serverService.updateById(serverEntity);
        }

        tokenEntity.setStatus("used");
        tokenEntity.setUsedAt(LocalDateTime.now());
        installTokenService.updateById(tokenEntity);

        statusPushService.publishServerStatus(serverEntity.getUserId(), serverEntity.getId(), serverEntity.getStatus(), "agent registered");

        return toRegisterResponse(serverEntity);
    }

    @Override
    public void heartbeat(AgentHeartbeatRequest request) {
        ServerEntity entity = serverService.getById(request.getServerId());
        if (entity == null) {
            throw BusinessException.notFound("server not found");
        }
        if (!request.getAgentId().equals(entity.getAgentId())) {
            throw BusinessException.unauthorized("agent identity mismatch");
        }
        entity.setStatus(request.getStatus());
        entity.setTailscaleIp(request.getTailscaleIp());
        entity.setLastHeartbeat(LocalDateTime.now());
        serverService.updateById(entity);
        statusPushService.publishServerStatus(entity.getUserId(), entity.getId(), entity.getStatus(), "heartbeat received");
        saveServerMetric(request);
    }

    @Override
    public AgentTaskResponse pullTask(AgentTaskPullRequest request) {
        ServerEntity serverEntity = assertAgentServer(request.getServerId(), request.getAgentId());
        TaskEntity taskEntity = taskService.getOne(new LambdaQueryWrapper<TaskEntity>()
                .eq(TaskEntity::getServerId, serverEntity.getId())
                .eq(TaskEntity::getStatus, TaskDto.STATUS_PENDING)
                .orderByAsc(TaskEntity::getId)
                .last("limit 1"));
        if (taskEntity == null) {
            return null;
        }
        taskEntity.setAgentId(request.getAgentId());
        taskEntity.setStatus(TaskDto.STATUS_RUNNING);
        if (taskEntity.getStartedAt() == null) {
            taskEntity.setStartedAt(LocalDateTime.now());
        }
        taskService.updateById(taskEntity);
        statusPushService.publishTaskStatus(serverEntity.getUserId(), taskEntity.getId(), taskEntity.getServerId(), taskEntity.getServiceId(), taskEntity.getStatus(), "task claimed by agent");
        return new AgentTaskResponse(taskEntity.getId(), taskEntity.getType(), fromJson(taskEntity.getPayload(), TaskPayloadDto.class));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reportTaskResult(Long taskId, AgentTaskResultRequest request) {
        TaskEntity taskEntity = taskService.getById(taskId);
        if (taskEntity == null) {
            throw BusinessException.notFound("task not found");
        }
        if (TaskDto.STATUS_CANCELLED.equalsIgnoreCase(taskEntity.getStatus())
                || TaskDto.STATUS_TIMED_OUT.equalsIgnoreCase(taskEntity.getStatus())) {
            throw BusinessException.conflict("task already closed");
        }
        if (!TaskDto.STATUS_RUNNING.equalsIgnoreCase(taskEntity.getStatus())) {
            throw BusinessException.conflict("task is not running");
        }
        ServerEntity serverEntity = assertAgentServer(taskEntity.getServerId(), request.getAgentId());
        if (!serverEntity.getAgentId().equals(request.getAgentId())) {
            throw BusinessException.unauthorized("agent identity mismatch");
        }
        taskEntity.setStatus(request.getStatus());
        taskEntity.setResult(toJson(request.getTaskResult()));
        taskEntity.setErrorMessage(request.getErrorMessage());
        taskEntity.setCompletedAt(LocalDateTime.now());
        taskService.updateById(taskEntity);
        statusPushService.publishTaskStatus(serverEntity.getUserId(), taskEntity.getId(), taskEntity.getServerId(), taskEntity.getServiceId(), taskEntity.getStatus(), "task result reported");
        syncServiceStatus(taskEntity, request);
    }

    @Override
    public void reportLog(AgentLogRequest request) {
        assertAgentServer(request.getServerId(), request.getAgentId());
        LogEntity entity = new LogEntity();
        entity.setAgentId(request.getAgentId());
        entity.setServerId(request.getServerId());
        entity.setServiceId(request.getServiceId());
        entity.setTaskId(request.getTaskId());
        entity.setLevel(request.getLevel());
        entity.setContent(request.getContent());
        entity.setSource(request.getSource());
        logService.save(entity);
    }

    private ServerEntity assertAgentServer(Long serverId, String agentId) {
        ServerEntity entity = serverService.getById(serverId);
        if (entity == null) {
            throw BusinessException.notFound("server not found");
        }
        if (entity.getAgentId() == null || !entity.getAgentId().equals(agentId)) {
            throw BusinessException.unauthorized("agent identity mismatch");
        }
        return entity;
    }

    private ServerEntity findServerByHostname(Long userId, String hostname) {
        return serverService.lambdaQuery()
                .eq(ServerEntity::getUserId, userId)
                .eq(ServerEntity::getHostname, hostname)
                .orderByDesc(ServerEntity::getId)
                .last("limit 1")
                .one();
    }

    private void fillServerRegistrationFields(
            ServerEntity serverEntity,
            InstallTokenEntity tokenEntity,
            AgentRegisterRequest request,
            String hostname
    ) {
        serverEntity.setHostname(hostname);
        serverEntity.setDisplayName(firstNonBlank(tokenEntity.getHostname(), hostname));
        serverEntity.setOs(request.getOs());
        serverEntity.setArch(request.getArch());
        serverEntity.setIp(request.getIp());
        serverEntity.setStatus("online");
        serverEntity.setAgentVersion(request.getAgentVersion());
        serverEntity.setCpu(request.getCpu());
        serverEntity.setMemory(request.getMemory());
        serverEntity.setDisk(request.getDisk());
        serverEntity.setLastHeartbeat(LocalDateTime.now());
    }

    private AgentRegisterResponse toRegisterResponse(ServerEntity serverEntity) {
        return new AgentRegisterResponse(serverEntity.getId(), serverEntity.getAgentId(), serverEntity.getHostname());
    }

    private String generateAgentId() {
        return "agent-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private void syncServiceStatus(TaskEntity taskEntity, AgentTaskResultRequest request) {
        if (taskEntity.getServiceId() == null) {
            return;
        }
        ServiceEntity serviceEntity = serviceBizService.getById(taskEntity.getServiceId());
        if (serviceEntity == null) {
            return;
        }
        boolean success = "success".equalsIgnoreCase(request.getStatus());
        String taskType = taskEntity.getType();

        if (success) {
            if ("START".equalsIgnoreCase(taskType) || "RESTART".equalsIgnoreCase(taskType)) {
                serviceEntity.setStatus("active");
                serviceEntity.setEnabled(1);
                if (request.getTaskResult() != null) {
                    String accessUrl = firstNonBlank(request.getTaskResult().getAccessUrl(), request.getTaskResult().getEndpoint());
                    if (accessUrl != null) {
                        serviceEntity.setAccessUrl(accessUrl);
                    }
                }
            } else if ("STOP".equalsIgnoreCase(taskType) || "DELETE".equalsIgnoreCase(taskType)) {
                serviceEntity.setStatus("inactive");
                serviceEntity.setEnabled(0);
            } else if ("CHECK".equalsIgnoreCase(taskType)) {
                serviceEntity.setHealthStatus("healthy");
                serviceEntity.setLastCheckAt(LocalDateTime.now());
                if (request.getTaskResult() != null) {
                    String accessUrl = firstNonBlank(request.getTaskResult().getAccessUrl(), request.getTaskResult().getEndpoint());
                    if (accessUrl != null) {
                        serviceEntity.setAccessUrl(accessUrl);
                    }
                }
            }
        } else {
            if ("CHECK".equalsIgnoreCase(taskType)) {
                serviceEntity.setHealthStatus("error");
                serviceEntity.setLastCheckAt(LocalDateTime.now());
            } else {
                serviceEntity.setStatus("error");
            }
        }
        serviceBizService.updateById(serviceEntity);
        ServerEntity serverEntity = serverService.getById(serviceEntity.getServerId());
        Long userId = serverEntity != null ? serverEntity.getUserId() : null;
        statusPushService.publishServiceStatus(userId, serviceEntity.getId(), serviceEntity.getServerId(), serviceEntity.getStatus(), serviceEntity.getHealthStatus(), "service status synchronized from task");
        if ("CHECK".equalsIgnoreCase(taskType)) {
            saveServiceMetric(serviceEntity, request, success);
        }
    }

    private void saveServerMetric(AgentHeartbeatRequest request) {
        if (request.getCpuUsage() == null
                && request.getMemoryUsage() == null
                && request.getDiskUsage() == null
                && request.getNetworkInBytes() == null
                && request.getNetworkOutBytes() == null
                && request.getLoadAverage() == null) {
            return;
        }
        ServerMetricEntity metricEntity = new ServerMetricEntity();
        metricEntity.setServerId(request.getServerId());
        metricEntity.setCpuUsage(request.getCpuUsage());
        metricEntity.setMemoryUsage(request.getMemoryUsage());
        metricEntity.setDiskUsage(request.getDiskUsage());
        metricEntity.setNetworkInBytes(request.getNetworkInBytes());
        metricEntity.setNetworkOutBytes(request.getNetworkOutBytes());
        metricEntity.setLoadAverage(request.getLoadAverage());
        serverMetricService.save(metricEntity);
    }

    private void saveServiceMetric(ServiceEntity serviceEntity, AgentTaskResultRequest request, boolean success) {
        ServiceMetricEntity metricEntity = new ServiceMetricEntity();
        metricEntity.setServiceId(serviceEntity.getId());
        metricEntity.setServerId(serviceEntity.getServerId());
        metricEntity.setStatus(success ? 1 : 0);
        TaskResultDto taskResult = request.getTaskResult();
        if (taskResult != null) {
            metricEntity.setResponseTimeMs(taskResult.getResponseTimeMs());
            metricEntity.setSuccessCount(taskResult.getSuccessCount());
            metricEntity.setErrorCount(taskResult.getErrorCount());
        }
        serviceMetricService.save(metricEntity);
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return null;
    }

    private <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw BusinessException.internal("failed to parse task payload");
        }
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw BusinessException.internal("failed to serialize task result");
        }
    }
}
