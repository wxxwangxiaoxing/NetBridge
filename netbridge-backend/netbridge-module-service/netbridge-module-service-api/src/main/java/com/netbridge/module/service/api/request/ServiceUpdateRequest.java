package com.netbridge.module.service.api.request;

import lombok.Data;

@Data
public class ServiceUpdateRequest {

    private Long id;
    private String name;
    private String type;
    private String protocol;
    private Integer port;
    private String targetHost;
    private String domain;
    private Integer enabled;
    private String status;
    private String accessType;
    private String accessUrl;
    private String healthStatus;
    private String remark;
}
