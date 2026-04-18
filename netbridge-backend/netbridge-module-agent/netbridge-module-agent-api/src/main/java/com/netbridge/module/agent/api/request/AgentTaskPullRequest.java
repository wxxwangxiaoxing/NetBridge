package com.netbridge.module.agent.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AgentTaskPullRequest {

    @NotBlank(message = "must not be blank")
    private String agentId;

    @NotNull(message = "must not be null")
    private Long serverId;
}
