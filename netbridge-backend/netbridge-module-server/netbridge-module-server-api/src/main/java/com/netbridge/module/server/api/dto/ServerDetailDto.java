package com.netbridge.module.server.api.dto;

import com.netbridge.module.log.api.dto.LogDto;
import com.netbridge.module.service.api.dto.ServiceDto;
import com.netbridge.module.task.api.dto.TaskDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ServerDetailDto extends ServerDto {

    private Long serviceTotal;
    private List<ServiceDto> services;
    private Long recentTaskTotal;
    private List<TaskDto> recentTasks;
    private Long recentLogTotal;
    private List<LogDto> recentLogs;
}
