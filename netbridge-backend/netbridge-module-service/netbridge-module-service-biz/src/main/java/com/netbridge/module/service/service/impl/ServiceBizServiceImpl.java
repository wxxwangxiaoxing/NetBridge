package com.netbridge.module.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.netbridge.module.service.entity.ServiceEntity;
import com.netbridge.module.service.mapper.ServiceMapper;
import com.netbridge.module.service.service.ServiceBizService;
import org.springframework.stereotype.Service;

@Service
public class ServiceBizServiceImpl extends ServiceImpl<ServiceMapper, ServiceEntity> implements ServiceBizService {
}
