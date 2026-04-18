package com.netbridge.module.monitor.api.request;

import lombok.Data;

@Data
public class ServerMetricQueryRequest {

    private Long pageNo = 1L;
    private Long pageSize = 20L;
    private Long serverId;
}
