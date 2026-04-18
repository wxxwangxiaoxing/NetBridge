package com.netbridge.module.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.netbridge.module.task.entity.TaskEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskMapper extends BaseMapper<TaskEntity> {
}
