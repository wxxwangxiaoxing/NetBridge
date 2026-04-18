package com.netbridge.module.task.api.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskDto {

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_RUNNING = "running";
    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_FAILED = "failed";
    public static final String STATUS_CANCELLED = "cancelled";
    public static final String STATUS_TIMED_OUT = "timed_out";

    private Long id;
    private Long serverId;
    private Long serviceId;
    private String agentId;
    private String type;
    private String status;
    private String payload;
    private TaskPayloadDto payloadObj;
    private String result;
    private TaskResultDto resultObj;
    private String errorMessage;
    private Integer retryCount;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
