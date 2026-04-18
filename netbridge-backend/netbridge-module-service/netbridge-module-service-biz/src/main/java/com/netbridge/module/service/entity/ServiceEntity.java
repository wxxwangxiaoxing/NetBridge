package com.netbridge.module.service.entity;

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
@TableName("service")
public class ServiceEntity extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("server_id")
    private Long serverId;

    private String name;

    private String type;

    private String protocol;

    private Integer port;

    @TableField("target_host")
    private String targetHost;

    private String domain;

    private String subdomain;

    private Integer enabled;

    private String status;

    @TableField("access_type")
    private String accessType;

    @TableField("access_url")
    private String accessUrl;

    @TableField("health_status")
    private String healthStatus;

    @TableField("last_check_at")
    private LocalDateTime lastCheckAt;

    private String remark;
}
