package com.netbridge.module.monitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.netbridge.module.monitor.entity.ServerMetricEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ServerMetricMapper extends BaseMapper<ServerMetricEntity> {
}
