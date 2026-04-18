package com.netbridge.module.agent.api.request;

import com.netbridge.module.task.api.dto.TaskResultDto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AgentTaskResultRequest {

    @NotBlank(message = "must not be blank")
    private String agentId;

    @NotBlank(message = "must not be blank")
    private String status;

    private TaskResultDto taskResult;

    private String errorMessage;
}
