package com.netbridge.module.tailscale.api.dto;

import lombok.Data;

@Data
public class TailscaleDeviceDto {

    private Long serverId;
    private String tailscaleIp;
    private String magicDnsName;
    private String status;
}
