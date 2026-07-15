package com.xiaoyang.d_game.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoyang.d_game.dto.UserFavoriteResp;
import com.xiaoyang.d_game.entity.UserFavorite;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
/**
 * 用户收藏 Mapper。
 */
public interface UserFavoriteMapper extends BaseMapper<UserFavorite> {

    /**
     * 物理删除指定收藏关系。
     *
     * <p>收藏关系和点赞关系一样有唯一键约束，取消收藏时需要释放唯一键，
     * 否则再次收藏同一目标会被历史逻辑删除记录挡住。</p>
     */
    @Delete("DELETE FROM user_favorite WHERE user_id = #{userId} AND target_type = #{targetType} AND target_id = #{targetId}")
    int deletePhysicallyByUserAndTarget(@Param("userId") Long userId,
                                        @Param("targetType") Integer targetType,
                                        @Param("targetId") Long targetId);

    /**
     * 统计某个用户公开收藏流里可见的收藏条数。
     *
     * <p>帖子要求审核通过且未删除；游戏要求未删除。这样统计结果和公开主页收藏 tab 完全一致。</p>
     */
    @Select("""
            SELECT COUNT(1)
            FROM (
                SELECT uf.id
                FROM user_favorite uf
                JOIN post p ON p.id = uf.target_id AND p.is_deleted = 0 AND p.status = 2
                JOIN `user` u ON u.id = p.user_id AND u.is_deleted = 0
                JOIN board b ON b.id = p.board_id AND b.is_deleted = 0
                LEFT JOIN game g ON g.id = p.game_id AND g.is_deleted = 0
                WHERE uf.user_id = #{userId} AND uf.target_type = 1 AND uf.is_deleted = 0
                UNION ALL
                SELECT uf.id
                FROM user_favorite uf
                JOIN game g ON g.id = uf.target_id AND g.is_deleted = 0
                JOIN game_category gc ON gc.id = g.category_id AND gc.is_deleted = 0
                WHERE uf.user_id = #{userId} AND uf.target_type = 3 AND uf.is_deleted = 0
            ) t
            """)
    Long countVisibleFavorites(@Param("userId") Long userId);

    /**
     * 分页查询某个用户公开收藏流。
     *
     * <p>结果按收藏时间倒序返回，并统一映射到一个 DTO，前端再根据 targetType 区分帖子和游戏。</p>
     */
    @Select("""
            SELECT *
            FROM (
                SELECT
                    uf.target_type AS targetType,
                    uf.target_id AS targetId,
                    p.title AS title,
                    p.content AS content,
                    NULL AS coverUrl,
                    p.board_id AS boardId,
                    b.name AS boardName,
                    p.game_id AS gameId,
                    g.name AS gameName,
                    p.user_id AS userId,
                    au.nickname AS authorNickname,
                    au.username AS authorUsername,
                    au.avatar_url AS authorAvatarUrl,
                    au.status AS authorStatus,
                    p.status AS status,
                    p.view_count AS viewCount,
                    p.like_count AS likeCount,
                    p.comment_count AS commentCount,
                    p.favorite_count AS favoriteCount,
                    NULL AS categoryName,
                    NULL AS avgRating,
                    NULL AS ratingCount,
                    uf.gmt_create AS gmtCreate
                FROM user_favorite uf
                JOIN post p ON p.id = uf.target_id AND p.is_deleted = 0 AND p.status = 2
                JOIN `user` au ON au.id = p.user_id AND au.is_deleted = 0
                JOIN board b ON b.id = p.board_id AND b.is_deleted = 0
                LEFT JOIN game g ON g.id = p.game_id AND g.is_deleted = 0
                WHERE uf.user_id = #{userId} AND uf.target_type = 1 AND uf.is_deleted = 0
                UNION ALL
                SELECT
                    uf.target_type AS targetType,
                    uf.target_id AS targetId,
                    g.name AS title,
                    g.description AS content,
                    g.cover_url AS coverUrl,
                    NULL AS boardId,
                    NULL AS boardName,
                    g.id AS gameId,
                    g.name AS gameName,
                    NULL AS userId,
                    NULL AS authorNickname,
                    NULL AS authorUsername,
                    NULL AS authorAvatarUrl,
                    NULL AS authorStatus,
                    NULL AS status,
                    NULL AS viewCount,
                    NULL AS likeCount,
                    NULL AS commentCount,
                    NULL AS favoriteCount,
                    gc.name AS categoryName,
                    g.avg_rating AS avgRating,
                    g.rating_count AS ratingCount,
                    uf.gmt_create AS gmtCreate
                FROM user_favorite uf
                JOIN game g ON g.id = uf.target_id AND g.is_deleted = 0
                JOIN game_category gc ON gc.id = g.category_id AND gc.is_deleted = 0
                WHERE uf.user_id = #{userId} AND uf.target_type = 3 AND uf.is_deleted = 0
            ) t
            ORDER BY gmtCreate DESC
            LIMIT #{size} OFFSET #{offset}
            """)
    List<UserFavoriteResp> selectVisibleFavorites(@Param("userId") Long userId,
                                                  @Param("offset") long offset,
                                                  @Param("size") long size);
}
