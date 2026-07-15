package com.xiaoyang.d_game.service;

import com.xiaoyang.d_game.common.PageResult;
import com.xiaoyang.d_game.common.enums.CommentSortTypeEnum;
import com.xiaoyang.d_game.dto.CommentCreateReq;
import com.xiaoyang.d_game.dto.CommentResp;
import com.xiaoyang.d_game.dto.InteractionStatusResp;

/**
 * 用户互动业务接口。
 *
 * <p>统一处理评论、点赞、收藏和关注。实现类会维护关系表唯一性、帖子/评论计数和站内通知。</p>
 */
public interface InteractService {

    /**
     * 创建评论并增加帖子评论数。
     */
    Long createComment(CommentCreateReq req);

    /**
     * 分页查询帖子评论。
     */
    PageResult<CommentResp> pageComments(Long postId, Long page, Long size, CommentSortTypeEnum sort);

    /**
     * 查询当前用户对目标的点赞和收藏状态。
     */
    InteractionStatusResp getStatus(Integer targetType, Long targetId);

    /**
     * 点赞目标。
     */
    void like(Integer targetType, Long targetId);

    /**
     * 取消点赞目标。
     */
    void unlike(Integer targetType, Long targetId);

    /**
     * 收藏目标。
     */
    void favorite(Integer targetType, Long targetId);

    /**
     * 取消收藏目标。
     */
    void unfavorite(Integer targetType, Long targetId);

    /**
     * 关注用户。
     */
    void follow(Long followeeId);

    /**
     * 取消关注用户。
     */
    void unfollow(Long followeeId);
}
