package com.netbridge.module.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.netbridge.framework.security.util.UserContextHolder;
import com.netbridge.framework.web.api.ApiResponse;
import com.netbridge.framework.web.api.PageResult;
import com.netbridge.framework.web.exception.BusinessException;
import com.netbridge.module.log.api.annotation.OperationAudit;
import com.netbridge.module.server.api.dto.InstallCommandResponse;
import com.netbridge.module.server.api.dto.InstallTokenDto;
import com.netbridge.module.server.api.dto.ServerDetailDto;
import com.netbridge.module.server.api.dto.ServerDto;
import com.netbridge.module.server.api.request.GenerateInstallCommandRequest;
import com.netbridge.module.server.api.request.InstallTokenQueryRequest;
import com.netbridge.module.server.api.request.ServerCreateRequest;
import com.netbridge.module.server.api.request.ServerQueryRequest;
import com.netbridge.module.server.api.request.ServerUpdateRequest;
import com.netbridge.module.log.api.dto.LogDto;
import com.netbridge.module.log.entity.LogEntity;
import com.netbridge.module.log.service.OperationLogService;
import com.netbridge.module.log.service.LogService;
import com.netbridge.module.service.api.dto.ServiceDto;
import com.netbridge.module.service.entity.ServiceEntity;
import com.netbridge.module.service.service.ServiceBizService;
import com.netbridge.module.server.entity.InstallTokenEntity;
import com.netbridge.module.server.entity.ServerEntity;
import com.netbridge.module.server.service.InstallTokenService;
import com.netbridge.module.server.service.ServerService;
import com.netbridge.module.task.api.dto.TaskDto;
import com.netbridge.module.task.entity.TaskEntity;
import com.netbridge.module.task.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/server")
public class ServerController {

    private final ServerService serverService;
    private final InstallTokenService installTokenService;
    private final ServiceBizService serviceBizService;
    private final TaskService taskService;
    private final LogService logService;
    private final OperationLogService operationLogService;
    private final String installScriptUrl;

    public ServerController(
            ServerService serverService,
            InstallTokenService installTokenService,
            ServiceBizService serviceBizService,
            TaskService taskService,
            LogService logService,
            OperationLogService operationLogService,
            @Value("${netbridge.agent.install-script-url:https://netbridge.com/install.sh}") String installScriptUrl
    ) {
        this.serverService = serverService;
        this.installTokenService = installTokenService;
        this.serviceBizService = serviceBizService;
        this.taskService = taskService;
        this.logService = logService;
        this.operationLogService = operationLogService;
        this.installScriptUrl = installScriptUrl;
    }

    @GetMapping("/list")
    public ApiResponse<PageResult<ServerDto>> list(ServerQueryRequest request) {
        LambdaQueryWrapper<ServerEntity> wrapper = new LambdaQueryWrapper<ServerEntity>()
                .eq(request.getUserId() != null, ServerEntity::getUserId, request.getUserId())
                .eq(request.getStatus() != null && !request.getStatus().isBlank(), ServerEntity::getStatus, request.getStatus())
                .like(request.getHostname() != null && !request.getHostname().isBlank(), ServerEntity::getHostname, request.getHostname())
                .orderByDesc(ServerEntity::getId);
        Page<ServerEntity> page = serverService.page(new Page<>(request.getPageNo(), request.getPageSize()), wrapper);
        return ApiResponse.success(new PageResult<>(
                page.getCurrent(),
                page.getSize(),
                page.getTotal(),
                page.getRecords().stream().map(this::toDto).toList()
        ));
    }

    @GetMapping("/{id}")
    public ApiResponse<ServerDto> getById(@PathVariable Long id) {
        return ApiResponse.success(toDto(serverService.getById(id)));
    }

    @GetMapping("/{id}/detail")
    public ApiResponse<ServerDetailDto> detail(@PathVariable Long id) {
        ServerEntity serverEntity = serverService.getById(id);
        if (serverEntity == null) {
            throw BusinessException.notFound("server not found");
        }
        ServerDetailDto dto = new ServerDetailDto();
        BeanUtils.copyProperties(serverEntity, dto);
        dto.setServiceTotal(serviceBizService.lambdaQuery().eq(ServiceEntity::getServerId, id).count());
        dto.setServices(serviceBizService.lambdaQuery()
                .eq(ServiceEntity::getServerId, id)
                .orderByDesc(ServiceEntity::getId)
                .list()
                .stream()
                .map(this::toServiceDto)
                .toList());
        dto.setRecentTaskTotal(taskService.lambdaQuery().eq(TaskEntity::getServerId, id).count());
        dto.setRecentTasks(taskService.lambdaQuery()
                .eq(TaskEntity::getServerId, id)
                .orderByDesc(TaskEntity::getId)
                .last("limit 10")
                .list()
                .stream()
                .map(this::toTaskDto)
                .toList());
        dto.setRecentLogTotal(logService.lambdaQuery().eq(LogEntity::getServerId, id).count());
        dto.setRecentLogs(logService.lambdaQuery()
                .eq(LogEntity::getServerId, id)
                .orderByDesc(LogEntity::getId)
                .last("limit 20")
                .list()
                .stream()
                .map(this::toLogDto)
                .toList());
        return ApiResponse.success(dto);
    }

    @PostMapping("/generate-command")
    @OperationAudit(
            action = "INSTALL_TOKEN_CREATE",
            resourceType = "install_token",
            successDetail = "'generated install token for ' + #request.hostname",
            failureDetail = "'failed to generate install token: ' + #errorMessage"
    )
    public ApiResponse<InstallCommandResponse> generateInstallCommand(@Valid @RequestBody GenerateInstallCommandRequest request) {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw BusinessException.unauthorized("unauthorized");
        }
        installTokenService.expireUnusedTokens();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(5);
        String token = UUID.randomUUID().toString().replace("-", "");

        InstallTokenEntity entity = new InstallTokenEntity();
        entity.setUserId(userId);
        entity.setToken(token);
        entity.setHostname(request.getHostname());
        entity.setStatus("unused");
        entity.setExpiresAt(expiresAt);
        installTokenService.save(entity);
        operationLogService.record(userId, currentUsername(), "INSTALL_TOKEN_CREATE", "install_token", entity.getId(), "success", "generated install token");

        String command = "curl -fsSL " + installScriptUrl + " | bash -s -- --token=" + token;
        return ApiResponse.success(new InstallCommandResponse(command, token, expiresAt));
    }

    @PostMapping("/install-token/cleanup")
    public ApiResponse<Long> cleanupExpiredInstallTokens() {
        return ApiResponse.success(installTokenService.expireUnusedTokens());
    }

    @GetMapping("/install-token/list")
    public ApiResponse<PageResult<InstallTokenDto>> listInstallTokens(InstallTokenQueryRequest request) {
        Long userId = requireUserId();
        installTokenService.expireUnusedTokens();
        LambdaQueryWrapper<InstallTokenEntity> wrapper = new LambdaQueryWrapper<InstallTokenEntity>()
                .eq(InstallTokenEntity::getUserId, userId)
                .eq(request.getStatus() != null && !request.getStatus().isBlank(), InstallTokenEntity::getStatus, request.getStatus())
                .like(request.getHostname() != null && !request.getHostname().isBlank(), InstallTokenEntity::getHostname, request.getHostname())
                .orderByDesc(InstallTokenEntity::getId);
        Page<InstallTokenEntity> page = installTokenService.page(new Page<>(request.getPageNo(), request.getPageSize()), wrapper);
        return ApiResponse.success(new PageResult<>(
                page.getCurrent(),
                page.getSize(),
                page.getTotal(),
                page.getRecords().stream().map(this::toInstallTokenDto).toList()
        ));
    }

    @GetMapping("/install-token/{id}")
    public ApiResponse<InstallTokenDto> getInstallToken(@PathVariable Long id) {
        Long userId = requireUserId();
        installTokenService.expireUnusedTokens();
        InstallTokenEntity entity = installTokenService.lambdaQuery()
                .eq(InstallTokenEntity::getId, id)
                .eq(InstallTokenEntity::getUserId, userId)
                .one();
        if (entity == null) {
            throw BusinessException.notFound("install token not found");
        }
        return ApiResponse.success(toInstallTokenDto(entity));
    }

    @PostMapping("/install-token/{id}/revoke")
    @OperationAudit(
            action = "INSTALL_TOKEN_REVOKE",
            resourceType = "install_token",
            resourceId = "#result.data.id",
            successDetail = "'revoked install token #' + #id",
            failureDetail = "'failed to revoke install token #' + #id + ': ' + #errorMessage"
            ,recordOnSuccess = true
    )
    public ApiResponse<InstallTokenDto> revokeInstallToken(@PathVariable Long id) {
        Long userId = requireUserId();
        InstallTokenEntity entity = installTokenService.revokeToken(userId, id);
        return ApiResponse.success(toInstallTokenDto(entity));
    }

    @PostMapping("/create")
    @OperationAudit(
            action = "SERVER_CREATE",
            resourceType = "server",
            resourceId = "#result.data.id",
            successDetail = "'created server ' + #request.hostname",
            failureDetail = "'failed to create server ' + #request.hostname + ': ' + #errorMessage"
            ,recordOnSuccess = true
    )
    public ApiResponse<ServerDto> create(@RequestBody ServerCreateRequest request) {
        ServerEntity entity = new ServerEntity();
        entity.setUserId(request.getUserId());
        entity.setHostname(request.getHostname());
        entity.setOs(request.getOs());
        entity.setIp(request.getIp());
        entity.setStatus("offline");
        serverService.save(entity);
        return ApiResponse.success(toDto(entity));
    }

    @PutMapping("/update")
    @OperationAudit(
            action = "SERVER_UPDATE",
            resourceType = "server",
            resourceId = "#request.id",
            successDetail = "'updated server #' + #request.id",
            failureDetail = "'failed to update server #' + #request.id + ': ' + #errorMessage"
            ,recordOnSuccess = true
    )
    public ApiResponse<ServerDto> update(@RequestBody ServerUpdateRequest request) {
        ServerEntity entity = serverService.getById(request.getId());
        if (entity == null) {
            throw BusinessException.notFound("server not found");
        }
        entity.setHostname(request.getHostname());
        entity.setDisplayName(request.getDisplayName());
        entity.setOs(request.getOs());
        entity.setIp(request.getIp());
        entity.setTailscaleIp(request.getTailscaleIp());
        entity.setStatus(request.getStatus());
        entity.setRemark(request.getRemark());
        serverService.updateById(entity);
        return ApiResponse.success(toDto(entity));
    }

    @DeleteMapping("/{id}")
    @OperationAudit(
            action = "SERVER_DELETE",
            resourceType = "server",
            resourceId = "#id",
            successDetail = "'deleted server #' + #id",
            failureDetail = "'failed to delete server #' + #id + ': ' + #errorMessage"
            ,recordOnSuccess = true
    )
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        return ApiResponse.success(serverService.removeById(id));
    }

    private ServerDto toDto(ServerEntity entity) {
        if (entity == null) {
            return null;
        }
        ServerDto dto = new ServerDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    private InstallTokenDto toInstallTokenDto(InstallTokenEntity entity) {
        if (entity == null) {
            return null;
        }
        InstallTokenDto dto = new InstallTokenDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    private ServiceDto toServiceDto(ServiceEntity entity) {
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

    private Long requireUserId() {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw BusinessException.unauthorized("unauthorized");
        }
        return userId;
    }
}
