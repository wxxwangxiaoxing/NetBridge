package com.netbridge.module.agent.api.dto;

import com.netbridge.module.task.api.dto.TaskPayloadDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentTaskResponse {

    private Long id;
    private String type;
    private TaskPayloadDto payload;
}
