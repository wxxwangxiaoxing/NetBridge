package com.netbridge.module.log.api.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OperationLogDto {

    private Long id;
    private Long userId;
    private String username;
    private String action;
    private String resourceType;
    private Long resourceId;
    private String result;
    private String detail;
    private String ipAddress;
    private LocalDateTime createdAt;
}
