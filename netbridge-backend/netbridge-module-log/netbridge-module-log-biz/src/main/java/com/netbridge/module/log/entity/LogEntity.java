package com.netbridge.module.log.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("log")
public class LogEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("server_id")
    private Long serverId;

    @TableField("service_id")
    private Long serviceId;

    @TableField("task_id")
    private Long taskId;

    @TableField("agent_id")
    private String agentId;

    private String level;

    private String source;

    private String content;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
