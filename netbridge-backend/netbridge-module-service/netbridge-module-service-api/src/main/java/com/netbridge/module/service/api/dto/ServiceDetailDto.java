package com.netbridge.module.service.api.dto;

import com.netbridge.module.log.api.dto.LogDto;
import com.netbridge.module.monitor.api.dto.ServiceMetricDto;
import com.netbridge.module.task.api.dto.TaskDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ServiceDetailDto extends ServiceDto {

    private Long recentTaskTotal;
    private List<TaskDto> recentTasks;
    private Long recentLogTotal;
    private List<LogDto> recentLogs;
    private List<ServiceMetricDto> metrics;
}
