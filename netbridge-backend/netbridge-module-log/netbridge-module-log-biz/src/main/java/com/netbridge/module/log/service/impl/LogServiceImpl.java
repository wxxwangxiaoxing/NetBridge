package com.netbridge.module.log.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.netbridge.module.log.entity.LogEntity;
import com.netbridge.module.log.mapper.LogMapper;
import com.netbridge.module.log.service.LogService;
import org.springframework.stereotype.Service;

@Service
public class LogServiceImpl extends ServiceImpl<LogMapper, LogEntity> implements LogService {
}
