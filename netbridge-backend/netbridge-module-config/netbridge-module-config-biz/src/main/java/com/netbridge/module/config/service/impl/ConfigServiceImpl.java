package com.netbridge.module.config.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.netbridge.module.config.entity.ConfigEntity;
import com.netbridge.module.config.mapper.ConfigMapper;
import com.netbridge.module.config.service.ConfigService;
import org.springframework.stereotype.Service;

@Service
public class ConfigServiceImpl extends ServiceImpl<ConfigMapper, ConfigEntity> implements ConfigService {
}
