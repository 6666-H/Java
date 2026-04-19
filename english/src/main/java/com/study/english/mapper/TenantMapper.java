package com.study.english.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.english.entity.Tenant;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TenantMapper extends BaseMapper<Tenant> {
}
