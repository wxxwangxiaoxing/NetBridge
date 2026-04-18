package com.netbridge.module.monitor.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.netbridge.module.monitor.entity.ServiceMetricEntity;
import com.netbridge.module.monitor.mapper.ServiceMetricMapper;
import com.netbridge.module.monitor.service.ServiceMetricService;
import org.springframework.stereotype.Service;

@Service
public class ServiceMetricServiceImpl extends ServiceImpl<ServiceMetricMapper, ServiceMetricEntity> implements ServiceMetricService {
}
