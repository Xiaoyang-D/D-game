package com.xiaoyang.d_game.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoyang.d_game.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/**
 * 评论 Mapper。
 */
public interface CommentMapper extends BaseMapper<Comment> {
}
