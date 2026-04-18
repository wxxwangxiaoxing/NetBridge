package com.netbridge.module.service.api.request;

import lombok.Data;

@Data
public class ServiceCreateRequest {

    private Long serverId;
    private String name;
    private String type;
    private String protocol;
    private Integer port;
    private String accessType;
    private String domain;
}
