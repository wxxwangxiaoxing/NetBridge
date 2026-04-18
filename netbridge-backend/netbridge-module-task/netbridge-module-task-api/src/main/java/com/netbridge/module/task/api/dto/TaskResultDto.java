package com.netbridge.module.task.api.dto;

import lombok.Data;

@Data
public class TaskResultDto {

    private Boolean success;
    private String accessUrl;
    private String endpoint;
    private String message;
    private String serviceStatus;
    private String portStatus;
    private String tunnelStatus;
    private String tailscaleStatus;
    private AccessOperationDto serviceAction;
    private AccessOperationDto tunnelAction;
    private AccessOperationDto tailscaleAction;
    private Boolean rollbackApplied;
    private Integer responseTimeMs;
    private Long successCount;
    private Long errorCount;
    private String checkedAt;
}
