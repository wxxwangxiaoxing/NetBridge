package com.netbridge.module.monitor.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.netbridge.framework.web.api.ApiResponse;
import com.netbridge.framework.web.api.PageResult;
import com.netbridge.module.monitor.api.dto.ServerMetricDto;
import com.netbridge.module.monitor.api.dto.ServiceMetricDto;
import com.netbridge.module.monitor.api.request.ServerMetricQueryRequest;
import com.netbridge.module.monitor.api.request.ServiceMetricQueryRequest;
import com.netbridge.module.monitor.entity.ServerMetricEntity;
import com.netbridge.module.monitor.entity.ServiceMetricEntity;
import com.netbridge.module.monitor.service.ServerMetricService;
import com.netbridge.module.monitor.service.ServiceMetricService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/monitor")
public class MonitorController {

    private final ServerMetricService serverMetricService;
    private final ServiceMetricService serviceMetricService;

    public MonitorController(ServerMetricService serverMetricService, ServiceMetricService serviceMetricService) {
        this.serverMetricService = serverMetricService;
        this.serviceMetricService = serviceMetricService;
    }

    @GetMapping("/server/list")
    public ApiResponse<PageResult<ServerMetricDto>> serverList(ServerMetricQueryRequest request) {
        LambdaQueryWrapper<ServerMetricEntity> wrapper = new LambdaQueryWrapper<ServerMetricEntity>()
                .eq(request.getServerId() != null, ServerMetricEntity::getServerId, request.getServerId())
                .orderByDesc(ServerMetricEntity::getCollectedAt);
        Page<ServerMetricEntity> page = serverMetricService.page(new Page<>(request.getPageNo(), request.getPageSize()), wrapper);
        return ApiResponse.success(new PageResult<>(
                page.getCurrent(),
                page.getSize(),
                page.getTotal(),
                page.getRecords().stream().map(this::toServerDto).toList()
        ));
    }

    @GetMapping("/server/{id}")
    public ApiResponse<ServerMetricDto> serverDetail(@PathVariable Long id) {
        return ApiResponse.success(toServerDto(serverMetricService.getById(id)));
    }

    @GetMapping("/service/list")
    public ApiResponse<PageResult<ServiceMetricDto>> serviceList(ServiceMetricQueryRequest request) {
        LambdaQueryWrapper<ServiceMetricEntity> wrapper = new LambdaQueryWrapper<ServiceMetricEntity>()
                .eq(request.getServiceId() != null, ServiceMetricEntity::getServiceId, request.getServiceId())
                .eq(request.getServerId() != null, ServiceMetricEntity::getServerId, request.getServerId())
                .orderByDesc(ServiceMetricEntity::getCollectedAt);
        Page<ServiceMetricEntity> page = serviceMetricService.page(new Page<>(request.getPageNo(), request.getPageSize()), wrapper);
        return ApiResponse.success(new PageResult<>(
                page.getCurrent(),
                page.getSize(),
                page.getTotal(),
                page.getRecords().stream().map(this::toServiceDto).toList()
        ));
    }

    @GetMapping("/service/{id}")
    public ApiResponse<ServiceMetricDto> serviceDetail(@PathVariable Long id) {
        return ApiResponse.success(toServiceDto(serviceMetricService.getById(id)));
    }

    private ServerMetricDto toServerDto(ServerMetricEntity entity) {
        if (entity == null) {
            return null;
        }
        ServerMetricDto dto = new ServerMetricDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    private ServiceMetricDto toServiceDto(ServiceMetricEntity entity) {
        if (entity == null) {
            return null;
        }
        ServiceMetricDto dto = new ServiceMetricDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }
}
