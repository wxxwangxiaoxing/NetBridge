package com.netbridge.framework.web.socket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class StatusPushService {

    private final SimpMessagingTemplate messagingTemplate;

    public StatusPushService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishServerStatus(Long userId, Long serverId, String status, String message) {
        if (userId == null) {
            return;
        }
        StatusPushMessage payload = new StatusPushMessage();
        payload.setType("server_status");
        payload.setEntityId(serverId);
        payload.setServerId(serverId);
        payload.setStatus(status);
        payload.setMessage(message);
        payload.setTimestamp(LocalDateTime.now());
        messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/server/status", payload);
    }

    public void publishTaskStatus(Long userId, Long taskId, Long serverId, Long serviceId, String status, String message) {
        if (userId == null) {
            return;
        }
        StatusPushMessage payload = new StatusPushMessage();
        payload.setType("task_status");
        payload.setEntityId(taskId);
        payload.setTaskId(taskId);
        payload.setServerId(serverId);
        payload.setServiceId(serviceId);
        payload.setStatus(status);
        payload.setMessage(message);
        payload.setTimestamp(LocalDateTime.now());
        messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/task/status", payload);
    }

    public void publishServiceStatus(Long userId, Long serviceId, Long serverId, String status, String healthStatus, String message) {
        if (userId == null) {
            return;
        }
        StatusPushMessage payload = new StatusPushMessage();
        payload.setType("service_status");
        payload.setEntityId(serviceId);
        payload.setServiceId(serviceId);
        payload.setServerId(serverId);
        payload.setStatus(status);
        payload.setHealthStatus(healthStatus);
        payload.setMessage(message);
        payload.setTimestamp(LocalDateTime.now());
        messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/service/status", payload);
    }
}
