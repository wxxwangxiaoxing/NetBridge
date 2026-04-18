package com.netbridge.module.server.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class InstallCommandResponse {

    private String command;
    private String token;
    private LocalDateTime expiresAt;
}
