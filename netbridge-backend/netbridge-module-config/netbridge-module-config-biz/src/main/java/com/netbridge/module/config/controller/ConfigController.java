package com.netbridge.module.config.controller;

import com.netbridge.framework.web.api.ApiResponse;
import com.netbridge.module.config.api.dto.ConfigDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    @GetMapping("/ping")
    public ApiResponse<List<ConfigDto>> ping() {
        ConfigDto dto = new ConfigDto();
        dto.setKey("domain_suffix");
        dto.setValue("netbridge.local");
        dto.setValueType("string");
        return ApiResponse.success(List.of(dto));
    }
}
