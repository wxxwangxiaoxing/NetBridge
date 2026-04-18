package com.netbridge.module.task.api.request;

import lombok.Data;

@Data
public class TaskCreateRequest {

    private Long serverId;
    private Long serviceId;
    private String agentId;
    private String type;
    private String payload;
}
