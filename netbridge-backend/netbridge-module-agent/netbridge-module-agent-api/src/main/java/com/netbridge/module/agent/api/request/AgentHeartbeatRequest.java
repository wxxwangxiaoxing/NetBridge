package com.netbridge.module.agent.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AgentHeartbeatRequest {

    @NotBlank(message = "must not be blank")
    private String agentId;

    @NotNull(message = "must not be null")
    private Long serverId;

    @NotBlank(message = "must not be blank")
    private String status;

    private String tailscaleIp;

    private Double cpuUsage;

    private Double memoryUsage;

    private Double diskUsage;

    private Long networkInBytes;

    private Long networkOutBytes;

    private Double loadAverage;
}
