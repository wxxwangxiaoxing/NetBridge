package com.netbridge.module.tailscale.controller;

import com.netbridge.framework.web.api.ApiResponse;
import com.netbridge.module.tailscale.api.dto.TailscaleDeviceDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tailscale")
public class TailscaleController {

    @GetMapping("/ping")
    public ApiResponse<List<TailscaleDeviceDto>> ping() {
        TailscaleDeviceDto dto = new TailscaleDeviceDto();
        dto.setServerId(1L);
        dto.setTailscaleIp("100.64.0.10");
        dto.setMagicDnsName("demo-server.tailnet");
        dto.setStatus("connected");
        return ApiResponse.success(List.of(dto));
    }
}
