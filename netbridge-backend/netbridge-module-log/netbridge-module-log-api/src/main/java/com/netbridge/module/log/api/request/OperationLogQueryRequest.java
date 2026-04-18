package com.netbridge.module.log.api.request;

import lombok.Data;

@Data
public class OperationLogQueryRequest {

    private Long pageNo = 1L;
    private Long pageSize = 10L;
    private Long userId;
    private String username;
    private String action;
    private String resourceType;
    private Long resourceId;
    private String result;
}
