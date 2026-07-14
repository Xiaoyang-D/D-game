package com.xiaoyang.d_game.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoyang.d_game.entity.Post;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/**
 * 帖子 Mapper。
 */
public interface PostMapper extends BaseMapper<Post> {
}
