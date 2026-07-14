package com.xiaoyang.d_game.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoyang.d_game.entity.UserFavorite;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/**
 * 用户收藏 Mapper。
 */
public interface UserFavoriteMapper extends BaseMapper<UserFavorite> {
}
