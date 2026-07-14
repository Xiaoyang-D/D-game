package com.xiaoyang.d_game.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoyang.d_game.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/**
 * 用户 Mapper。
 */
public interface UserMapper extends BaseMapper<User> {
}

