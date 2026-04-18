package com.netbridge.module.service.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ServiceActionResponse {

    private Long taskId;
    private String taskType;
    private String taskStatus;
}
