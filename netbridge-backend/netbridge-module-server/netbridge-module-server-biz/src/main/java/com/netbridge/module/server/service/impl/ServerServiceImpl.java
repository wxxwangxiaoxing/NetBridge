package com.netbridge.module.server.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.netbridge.module.server.entity.ServerEntity;
import com.netbridge.module.server.mapper.ServerMapper;
import com.netbridge.module.server.service.ServerService;
import org.springframework.stereotype.Service;

@Service
public class ServerServiceImpl extends ServiceImpl<ServerMapper, ServerEntity> implements ServerService {
}
