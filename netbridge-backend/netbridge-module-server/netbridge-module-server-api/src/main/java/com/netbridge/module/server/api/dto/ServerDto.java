package com.netbridge.module.server.api.dto;

import lombok.Data;

@Data
public class ServerDto {

    private Long id;
    private Long userId;
    private String hostname;
    private String ip;
    private String tailscaleIp;
    private String status;
}
