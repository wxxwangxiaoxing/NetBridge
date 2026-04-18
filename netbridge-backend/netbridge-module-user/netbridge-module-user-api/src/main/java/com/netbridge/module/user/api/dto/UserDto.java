package com.netbridge.module.user.api.dto;

import lombok.Data;

@Data
public class UserDto {

    private Long id;
    private String username;
    private String email;
    private String role;
    private Integer status;
}
