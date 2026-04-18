package com.netbridge.module.tunnel.controller;

import com.netbridge.framework.web.api.ApiResponse;
import com.netbridge.module.tunnel.api.dto.TunnelDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tunnel")
public class TunnelController {

    @GetMapping("/ping")
    public ApiResponse<List<TunnelDto>> ping() {
        TunnelDto dto = new TunnelDto();
        dto.setServiceId(1L);
        dto.setDomain("app.netbridge.local");
        dto.setStatus("inactive");
        return ApiResponse.success(List.of(dto));
    }
}
