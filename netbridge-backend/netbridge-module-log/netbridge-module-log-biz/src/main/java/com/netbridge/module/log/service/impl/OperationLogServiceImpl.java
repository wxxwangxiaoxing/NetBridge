package com.netbridge.module.log.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.netbridge.module.log.entity.OperationLogEntity;
import com.netbridge.module.log.mapper.OperationLogMapper;
import com.netbridge.module.log.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLogEntity> implements OperationLogService {

    @Override
    public void record(Long userId, String username, String action, String resourceType, Long resourceId, String result, String detail) {
        OperationLogEntity entity = new OperationLogEntity();
        entity.setUserId(userId);
        entity.setUsername(username);
        entity.setAction(action);
        entity.setResourceType(resourceType);
        entity.setResourceId(resourceId);
        entity.setResult(result);
        entity.setDetail(detail);
        entity.setIpAddress(resolveIpAddress());
        save(entity);
    }

    private String resolveIpAddress() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (!(requestAttributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            return null;
        }
        HttpServletRequest request = servletRequestAttributes.getRequest();
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
