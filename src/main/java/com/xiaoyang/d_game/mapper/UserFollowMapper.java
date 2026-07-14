package com.xiaoyang.d_game.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoyang.d_game.entity.UserFollow;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/**
 * 用户关注 Mapper。
 */
public interface UserFollowMapper extends BaseMapper<UserFollow> {
}
