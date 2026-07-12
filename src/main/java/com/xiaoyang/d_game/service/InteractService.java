package com.xiaoyang.d_game.service;

import com.xiaoyang.d_game.common.PageResult;
import com.xiaoyang.d_game.dto.CommentCreateReq;
import com.xiaoyang.d_game.dto.CommentResp;

public interface InteractService {

    Long createComment(CommentCreateReq req);

    PageResult<CommentResp> pageComments(Long postId, Long page, Long size);

    void like(Integer targetType, Long targetId);

    void unlike(Integer targetType, Long targetId);

    void favorite(Integer targetType, Long targetId);

    void unfavorite(Integer targetType, Long targetId);

    void follow(Long followeeId);

    void unfollow(Long followeeId);
}
