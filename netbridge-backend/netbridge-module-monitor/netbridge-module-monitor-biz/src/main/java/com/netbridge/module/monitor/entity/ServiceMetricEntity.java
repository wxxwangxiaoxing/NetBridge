package com.netbridge.module.monitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("service_metric")
public class ServiceMetricEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("service_id")
    private Long serviceId;

    @TableField("server_id")
    private Long serverId;

    private Integer status;

    @TableField("response_time_ms")
    private Integer responseTimeMs;

    @TableField("success_count")
    private Long successCount;

    @TableField("error_count")
    private Long errorCount;

    @TableField("collected_at")
    private LocalDateTime collectedAt;
}
