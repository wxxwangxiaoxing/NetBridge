package com.netbridge.module.server.api.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InstallTokenDto {

    private Long id;
    private Long userId;
    private String token;
    private String hostname;
    private String status;
    private LocalDateTime expiresAt;
    private LocalDateTime usedAt;
    private LocalDateTime createdAt;
}
