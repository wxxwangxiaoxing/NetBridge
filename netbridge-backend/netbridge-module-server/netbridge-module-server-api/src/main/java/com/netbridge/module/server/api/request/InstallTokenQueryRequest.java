package com.netbridge.module.server.api.request;

import lombok.Data;

@Data
public class InstallTokenQueryRequest {

    private Long pageNo = 1L;
    private Long pageSize = 10L;
    private String status;
    private String hostname;
}
