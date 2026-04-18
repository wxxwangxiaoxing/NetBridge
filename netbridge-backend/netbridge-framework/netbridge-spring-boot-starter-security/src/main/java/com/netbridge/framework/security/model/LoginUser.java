package com.netbridge.framework.security.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.security.Principal;

@Data
@AllArgsConstructor
public class LoginUser implements Principal {

    private Long userId;
    private String username;

    @Override
    public String getName() {
        return userId == null ? username : String.valueOf(userId);
    }
}
