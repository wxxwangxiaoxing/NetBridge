package com.netbridge.module.log.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.netbridge.framework.web.api.ApiResponse;
import com.netbridge.framework.web.api.PageResult;
import com.netbridge.module.log.api.dto.LogDto;
import com.netbridge.module.log.api.dto.OperationLogDto;
import com.netbridge.module.log.api.request.LogQueryRequest;
import com.netbridge.module.log.api.request.OperationLogQueryRequest;
import com.netbridge.module.log.entity.LogEntity;
import com.netbridge.module.log.entity.OperationLogEntity;
import com.netbridge.module.log.service.LogService;
import com.netbridge.module.log.service.OperationLogService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/log")
public class LogController {

    private final LogService logService;
    private final OperationLogService operationLogService;

    public LogController(LogService logService, OperationLogService operationLogService) {
        this.logService = logService;
        this.operationLogService = operationLogService;
    }

    @GetMapping("/list")
    public ApiResponse<PageResult<LogDto>> list(LogQueryRequest request) {
        LambdaQueryWrapper<LogEntity> wrapper = new LambdaQueryWrapper<LogEntity>()
                .eq(request.getServerId() != null, LogEntity::getServerId, request.getServerId())
                .eq(request.getServiceId() != null, LogEntity::getServiceId, request.getServiceId())
                .eq(request.getTaskId() != null, LogEntity::getTaskId, request.getTaskId())
                .eq(request.getLevel() != null && !request.getLevel().isBlank(), LogEntity::getLevel, request.getLevel())
                .eq(request.getSource() != null && !request.getSource().isBlank(), LogEntity::getSource, request.getSource())
                .orderByDesc(LogEntity::getId);
        Page<LogEntity> page = logService.page(new Page<>(request.getPageNo(), request.getPageSize()), wrapper);
        return ApiResponse.success(new PageResult<>(
                page.getCurrent(),
                page.getSize(),
                page.getTotal(),
                page.getRecords().stream().map(this::toDto).toList()
        ));
    }

    @GetMapping("/{id}")
    public ApiResponse<LogDto> getById(@PathVariable Long id) {
        return ApiResponse.success(toDto(logService.getById(id)));
    }

    @GetMapping("/operation/list")
    public ApiResponse<PageResult<OperationLogDto>> listOperationLogs(OperationLogQueryRequest request) {
        LambdaQueryWrapper<OperationLogEntity> wrapper = new LambdaQueryWrapper<OperationLogEntity>()
                .eq(request.getUserId() != null, OperationLogEntity::getUserId, request.getUserId())
                .like(request.getUsername() != null && !request.getUsername().isBlank(), OperationLogEntity::getUsername, request.getUsername())
                .eq(request.getAction() != null && !request.getAction().isBlank(), OperationLogEntity::getAction, request.getAction())
                .eq(request.getResourceType() != null && !request.getResourceType().isBlank(), OperationLogEntity::getResourceType, request.getResourceType())
                .eq(request.getResourceId() != null, OperationLogEntity::getResourceId, request.getResourceId())
                .eq(request.getResult() != null && !request.getResult().isBlank(), OperationLogEntity::getResult, request.getResult())
                .orderByDesc(OperationLogEntity::getId);
        Page<OperationLogEntity> page = operationLogService.page(new Page<>(request.getPageNo(), request.getPageSize()), wrapper);
        return ApiResponse.success(new PageResult<>(
                page.getCurrent(),
                page.getSize(),
                page.getTotal(),
                page.getRecords().stream().map(this::toOperationDto).toList()
        ));
    }

    @GetMapping("/operation/{id}")
    public ApiResponse<OperationLogDto> getOperationLogById(@PathVariable Long id) {
        return ApiResponse.success(toOperationDto(operationLogService.getById(id)));
    }

    private LogDto toDto(LogEntity entity) {
        if (entity == null) {
            return null;
        }
        LogDto dto = new LogDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    private OperationLogDto toOperationDto(OperationLogEntity entity) {
        if (entity == null) {
            return null;
        }
        OperationLogDto dto = new OperationLogDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }
}
