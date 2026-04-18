package com.netbridge.module.task.api.request;

import lombok.Data;

@Data
public class TaskQueryRequest {

    private Long pageNo = 1L;
    private Long pageSize = 10L;
    private Long serverId;
    private Long serviceId;
    private String status;
    private String type;
}
