package com.netbridge.module.log.api.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LogDto {

    private Long id;
    private Long serverId;
    private Long serviceId;
    private Long taskId;
    private String agentId;
    private String level;
    private String source;
    private String content;
    private LocalDateTime createdAt;
}
