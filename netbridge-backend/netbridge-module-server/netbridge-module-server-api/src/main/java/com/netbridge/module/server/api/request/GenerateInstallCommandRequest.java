package com.netbridge.module.server.api.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GenerateInstallCommandRequest {

    @NotBlank(message = "must not be blank")
    private String hostname;
}
