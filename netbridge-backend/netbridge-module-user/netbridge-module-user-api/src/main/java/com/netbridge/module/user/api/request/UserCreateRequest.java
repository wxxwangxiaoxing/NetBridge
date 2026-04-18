package com.netbridge.module.user.api.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserCreateRequest {

    @NotBlank(message = "must not be blank")
    private String username;
    @NotBlank(message = "must not be blank")
    private String password;
    @Email(message = "is invalid")
    @NotBlank(message = "must not be blank")
    private String email;
    private String role;
}
