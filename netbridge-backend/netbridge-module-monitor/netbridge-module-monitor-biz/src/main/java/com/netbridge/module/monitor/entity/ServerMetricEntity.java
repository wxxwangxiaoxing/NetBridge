package com.netbridge.module.monitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("server_metric")
public class ServerMetricEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("server_id")
    private Long serverId;

    @TableField("cpu_usage")
    private Double cpuUsage;

    @TableField("memory_usage")
    private Double memoryUsage;

    @TableField("disk_usage")
    private Double diskUsage;

    @TableField("network_in_bytes")
    private Long networkInBytes;

    @TableField("network_out_bytes")
    private Long networkOutBytes;

    @TableField("load_average")
    private Double loadAverage;

    @TableField("collected_at")
    private LocalDateTime collectedAt;
}
