package com.xiaoyang.d_game.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiaoyang.d_game.common.PageResult;
import com.xiaoyang.d_game.dto.BannedAuthorPostQueryReq;
import com.xiaoyang.d_game.dto.PostCreateReq;
import com.xiaoyang.d_game.dto.PostQueryReq;
import com.xiaoyang.d_game.dto.PostRankingQueryReq;
import com.xiaoyang.d_game.dto.PostResp;
import com.xiaoyang.d_game.entity.Post;

/**
 * 帖子业务接口。
 *
 * <p>覆盖公开列表、排行榜、后台审核列表、个人帖子、关注流、详情和发帖。
 * 实现类会处理可见性规则、浏览数增加、富文本清洗和审核状态。</p>
 */
public interface PostService extends IService<Post> {

    /**
     * 分页查询公开帖子。
     */
    PageResult<PostResp> pagePosts(PostQueryReq req);

    /**
     * 分页查询帖子排行榜。
     */
    PageResult<PostResp> pageRanking(PostRankingQueryReq req);

    /**
     * 分页查询待审核帖子，供后台审核使用。
     */
    PageResult<PostResp> pagePendingPosts(Long page, Long size);

    /**
     * 查询封禁作者发布过的帖子。
     */
    PageResult<PostResp> pagePostsByBannedAuthors(BannedAuthorPostQueryReq req);

    /**
     * 查询当前登录用户自己的帖子。
     */
    PageResult<PostResp> pageMyPosts(Long page, Long size);

    /**
     * 查询当前用户关注对象发布的公开帖子。
     */
    PageResult<PostResp> pageFollowingPosts(Long page, Long size);

    /**
     * 查询帖子详情并增加浏览数。
     */
    PostResp getPostDetail(Long postId);

    /**
     * 创建帖子，默认进入待审核状态。
     */
    String createPost(PostCreateReq req);
}
