package com.netbridge.module.log.api.request;

import lombok.Data;

@Data
public class LogQueryRequest {

    private Long pageNo = 1L;
    private Long pageSize = 10L;
    private Long serverId;
    private Long serviceId;
    private Long taskId;
    private String level;
    private String source;
}
