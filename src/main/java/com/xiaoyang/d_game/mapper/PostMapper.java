package com.xiaoyang.d_game.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoyang.d_game.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
/**
 * 帖子 Mapper。
 */
public interface PostMapper extends BaseMapper<Post> {

    /**
     * 统计某个用户公开帖子收到的总点赞数。
     *
     * <p>这里只统计已经通过审核且未被逻辑删除的帖子，和公开主页展示口径保持一致。</p>
     */
    @Select("SELECT COALESCE(SUM(like_count), 0) FROM post "
            + "WHERE user_id = #{userId} AND status = 2 AND is_deleted = 0")
    Long sumPublishedLikeCountByUser(@Param("userId") Long userId);
}
