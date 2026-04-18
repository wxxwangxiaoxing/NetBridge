package com.netbridge.module.agent.api.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AgentRegisterRequest {

    @NotBlank(message = "must not be blank")
    private String token;

    @NotBlank(message = "must not be blank")
    private String hostname;

    @NotBlank(message = "must not be blank")
    private String os;

    @NotBlank(message = "must not be blank")
    private String ip;

    @NotBlank(message = "must not be blank")
    private String agentVersion;

    private String cpu;
    private String memory;
    private String disk;
    private String arch;
}
