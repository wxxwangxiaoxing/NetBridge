package com.netbridge.module.user.api.request;

import lombok.Data;

@Data
public class UserQueryRequest {

    private Long pageNo = 1L;
    private Long pageSize = 10L;
    private String username;
    private Integer status;
}
