package com.netbridge.module.log.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.netbridge.module.log.entity.OperationLogEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLogEntity> {
}
