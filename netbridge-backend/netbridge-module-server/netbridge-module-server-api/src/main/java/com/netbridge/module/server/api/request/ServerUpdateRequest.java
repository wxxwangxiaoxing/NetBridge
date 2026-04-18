package com.netbridge.module.server.api.request;

import lombok.Data;

@Data
public class ServerUpdateRequest {

    private Long id;
    private String hostname;
    private String displayName;
    private String os;
    private String ip;
    private String tailscaleIp;
    private String status;
    private String remark;
}
