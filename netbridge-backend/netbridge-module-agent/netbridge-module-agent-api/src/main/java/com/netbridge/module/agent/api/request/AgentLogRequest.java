package com.netbridge.module.agent.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AgentLogRequest {

    @NotBlank(message = "must not be blank")
    private String agentId;

    @NotNull(message = "must not be null")
    private Long serverId;

    private Long serviceId;

    private Long taskId;

    @NotBlank(message = "must not be blank")
    private String level;

    @NotBlank(message = "must not be blank")
    private String content;

    @NotBlank(message = "must not be blank")
    private String source;
}
