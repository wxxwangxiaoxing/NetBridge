package com.netbridge.module.task.api.request;

import lombok.Data;

@Data
public class TaskUpdateRequest {

    private Long id;
    private String status;
    private String result;
    private String errorMessage;
    private Integer retryCount;
}
