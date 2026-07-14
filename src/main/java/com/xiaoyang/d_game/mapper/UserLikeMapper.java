package com.xiaoyang.d_game.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoyang.d_game.entity.UserLike;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/**
 * 用户点赞 Mapper。
 */
public interface UserLikeMapper extends BaseMapper<UserLike> {
}
