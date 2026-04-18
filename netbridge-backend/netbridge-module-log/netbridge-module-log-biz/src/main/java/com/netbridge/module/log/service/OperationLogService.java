package com.netbridge.module.log.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.netbridge.module.log.entity.OperationLogEntity;

public interface OperationLogService extends IService<OperationLogEntity> {

    void record(Long userId, String username, String action, String resourceType, Long resourceId, String result, String detail);
}
