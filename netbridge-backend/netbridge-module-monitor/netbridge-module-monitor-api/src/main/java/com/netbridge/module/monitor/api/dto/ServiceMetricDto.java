package com.netbridge.module.monitor.api.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ServiceMetricDto {

    private Long id;
    private Long serviceId;
    private Long serverId;
    private Integer status;
    private Integer responseTimeMs;
    private Long successCount;
    private Long errorCount;
    private LocalDateTime collectedAt;
}
