package com.netbridge.module.service.api.request;

import lombok.Data;

@Data
public class ServiceQueryRequest {

    private Long pageNo = 1L;
    private Long pageSize = 10L;
    private Long serverId;
    private String type;
    private String status;
}
