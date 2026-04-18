package com.netbridge.module.user.api.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserUpdateRequest {

    @NotNull(message = "must not be null")
    private Long id;
    private String username;
    private String email;
    private String role;
    private Integer status;
}
