package com.netbridge.module.monitor.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.netbridge.module.monitor.entity.ServerMetricEntity;
import com.netbridge.module.monitor.mapper.ServerMetricMapper;
import com.netbridge.module.monitor.service.ServerMetricService;
import org.springframework.stereotype.Service;

@Service
public class ServerMetricServiceImpl extends ServiceImpl<ServerMetricMapper, ServerMetricEntity> implements ServerMetricService {
}
