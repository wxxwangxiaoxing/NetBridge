package com.netbridge.module.server.api.request;

import lombok.Data;

@Data
public class ServerCreateRequest {

    private Long userId;
    private String hostname;
    private String os;
    private String ip;
}
