package com.netbridge.module.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.netbridge.module.server.entity.ServerEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ServerMapper extends BaseMapper<ServerEntity> {
}
