package com.netbridge.module.task.api.dto;

import lombok.Data;

@Data
public class AccessOperationDto {

    private String component;
    private String action;
    private String mode;
    private String status;
    private String detail;
}
