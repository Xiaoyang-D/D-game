package com.xiaoyang.d_game.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoyang.d_game.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/**
 * 审计日志 Mapper。
 *
 * <p>继承 BaseMapper 获得审计日志表的基础 CRUD 和分页查询能力。</p>
 */
public interface AuditLogMapper extends BaseMapper<AuditLog> {
}
