package com.netbridge.module.task.api.dto;

import lombok.Data;

@Data
public class TaskPayloadDto {

    private Long serviceId;
    private Long serverId;
    private String action;
    private String serviceName;
    private Integer port;
    private String protocol;
    private String accessType;
    private String targetHost;
    private String domain;
}
