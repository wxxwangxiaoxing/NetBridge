package com.netbridge.framework.web.socket;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StatusPushMessage {

    private String type;
    private Long entityId;
    private Long serverId;
    private Long serviceId;
    private Long taskId;
    private String status;
    private String healthStatus;
    private String message;
    private LocalDateTime timestamp;
}
