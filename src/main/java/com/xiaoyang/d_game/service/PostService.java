package com.xiaoyang.d_game.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiaoyang.d_game.common.PageResult;
import com.xiaoyang.d_game.dto.BannedAuthorPostQueryReq;
import com.xiaoyang.d_game.dto.PostCreateReq;
import com.xiaoyang.d_game.dto.PostQueryReq;
import com.xiaoyang.d_game.dto.PostRankingQueryReq;
import com.xiaoyang.d_game.dto.PostResp;
import com.xiaoyang.d_game.entity.Post;

public interface PostService extends IService<Post> {

    PageResult<PostResp> pagePosts(PostQueryReq req);

    PageResult<PostResp> pageRanking(PostRankingQueryReq req);

    PageResult<PostResp> pagePendingPosts(Long page, Long size);

    PageResult<PostResp> pagePostsByBannedAuthors(BannedAuthorPostQueryReq req);

    PageResult<PostResp> pageMyPosts(Long page, Long size);

    PageResult<PostResp> pageFollowingPosts(Long page, Long size);

    PostResp getPostDetail(Long postId);

    String createPost(PostCreateReq req);
}
