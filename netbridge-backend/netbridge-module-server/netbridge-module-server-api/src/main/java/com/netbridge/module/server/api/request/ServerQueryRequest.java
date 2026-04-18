package com.netbridge.module.server.api.request;

import lombok.Data;

@Data
public class ServerQueryRequest {

    private Long pageNo = 1L;
    private Long pageSize = 10L;
    private Long userId;
    private String status;
    private String hostname;
}
