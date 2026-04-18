package com.netbridge.module.tunnel.api.dto;

import lombok.Data;

@Data
public class TunnelDto {

    private Long serviceId;
    private String domain;
    private String status;
}
