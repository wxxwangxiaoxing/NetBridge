package com.netbridge.module.config.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.netbridge.module.config.entity.ConfigEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ConfigMapper extends BaseMapper<ConfigEntity> {
}
