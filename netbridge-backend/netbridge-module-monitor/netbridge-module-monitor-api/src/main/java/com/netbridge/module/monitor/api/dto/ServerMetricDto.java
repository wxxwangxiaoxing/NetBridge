package com.netbridge.module.monitor.api.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ServerMetricDto {

    private Long id;
    private Long serverId;
    private Double cpuUsage;
    private Double memoryUsage;
    private Double diskUsage;
    private Long networkInBytes;
    private Long networkOutBytes;
    private Double loadAverage;
    private LocalDateTime collectedAt;
}
