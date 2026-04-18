package com.netbridge.module.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.netbridge.module.service.entity.ServiceEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ServiceMapper extends BaseMapper<ServiceEntity> {
}
