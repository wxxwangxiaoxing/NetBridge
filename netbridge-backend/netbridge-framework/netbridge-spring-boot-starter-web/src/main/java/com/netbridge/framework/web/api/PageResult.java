package com.netbridge.framework.web.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    private long pageNo;
    private long pageSize;
    private long total;
    private List<T> list;
}
