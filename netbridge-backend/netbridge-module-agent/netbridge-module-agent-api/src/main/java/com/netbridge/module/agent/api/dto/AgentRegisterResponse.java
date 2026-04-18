package com.netbridge.module.agent.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AgentRegisterResponse {

    private Long serverId;
    private String agentId;
    private String hostname;
}
