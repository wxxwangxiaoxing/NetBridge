package com.netbridge.module.monitor.api.request;

import lombok.Data;

@Data
public class ServiceMetricQueryRequest {

    private Long pageNo = 1L;
    private Long pageSize = 20L;
    private Long serviceId;
    private Long serverId;
}
