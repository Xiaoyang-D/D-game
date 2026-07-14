package com.xiaoyang.d_game.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoyang.d_game.entity.UserPointLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/**
 * 用户积分流水 Mapper。
 */
public interface UserPointLogMapper extends BaseMapper<UserPointLog> {
}
