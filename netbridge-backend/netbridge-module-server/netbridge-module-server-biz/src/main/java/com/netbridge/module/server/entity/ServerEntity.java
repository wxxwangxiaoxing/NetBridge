package com.netbridge.module.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.netbridge.framework.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("server")
public class ServerEntity extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("agent_id")
    private String agentId;

    private String hostname;

    @TableField("display_name")
    private String displayName;

    private String os;

    private String arch;

    private String ip;

    @TableField("tailscale_ip")
    private String tailscaleIp;

    private String status;

    @TableField("agent_version")
    private String agentVersion;

    private String cpu;

    private String memory;

    private String disk;

    @TableField("last_heartbeat")
    private LocalDateTime lastHeartbeat;

    private String remark;
}
