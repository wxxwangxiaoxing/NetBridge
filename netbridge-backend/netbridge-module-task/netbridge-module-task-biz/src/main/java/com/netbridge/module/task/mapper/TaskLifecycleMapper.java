package com.netbridge.module.task.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface TaskLifecycleMapper {

    @Update("""
            UPDATE service
            SET health_status = #{healthStatus},
                last_check_at = #{lastCheckAt}
            WHERE id = #{serviceId}
            """)
    int updateServiceHealth(
            @Param("serviceId") Long serviceId,
            @Param("healthStatus") String healthStatus,
            @Param("lastCheckAt") LocalDateTime lastCheckAt
    );

    @org.apache.ibatis.annotations.Select("""
            SELECT user_id
            FROM server
            WHERE id = #{serverId}
            """)
    Long findUserIdByServerId(@Param("serverId") Long serverId);

    @Update("""
            UPDATE service
            SET status = #{status},
                health_status = #{healthStatus}
            WHERE id = #{serviceId}
            """)
    int updateServiceStatusAndHealth(
            @Param("serviceId") Long serviceId,
            @Param("status") String status,
            @Param("healthStatus") String healthStatus
    );
}
