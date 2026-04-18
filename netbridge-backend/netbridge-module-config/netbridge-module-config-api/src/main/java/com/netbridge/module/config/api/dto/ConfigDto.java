package com.netbridge.module.config.api.dto;

import lombok.Data;

@Data
public class ConfigDto {

    private String key;
    private String value;
    private String valueType;
}
