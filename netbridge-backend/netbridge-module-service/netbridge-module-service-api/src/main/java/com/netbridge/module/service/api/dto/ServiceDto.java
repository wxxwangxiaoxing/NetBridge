package com.netbridge.module.service.api.dto;

import lombok.Data;

@Data
public class ServiceDto {

    private Long id;
    private Long serverId;
    private String name;
    private String type;
    private String protocol;
    private Integer port;
    private String targetHost;
    private String accessType;
    private String status;
    private String domain;
    private String accessUrl;
    private String healthStatus;
}
