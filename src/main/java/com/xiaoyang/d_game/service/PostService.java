package com.xiaoyang.d_game.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiaoyang.d_game.common.PageResult;
import com.xiaoyang.d_game.dto.BannedAuthorPostQueryReq;
import com.xiaoyang.d_game.dto.PostCreateReq;
import com.xiaoyang.d_game.dto.PostCollectionResp;
import com.xiaoyang.d_game.dto.PostDraftResp;
import com.xiaoyang.d_game.dto.PostDraftSaveReq;
import com.xiaoyang.d_game.dto.PostManageItemResp;
import com.xiaoyang.d_game.dto.PostManagePageResp;
import com.xiaoyang.d_game.dto.PostQueryReq;
import com.xiaoyang.d_game.dto.PostRankingQueryReq;
import com.xiaoyang.d_game.dto.PostResp;
import com.xiaoyang.d_game.dto.PostPublishReq;
import com.xiaoyang.d_game.dto.PostTopicResp;
import com.xiaoyang.d_game.entity.Post;

import java.util.List;

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

    /** 管理员查看已提交帖子，排除草稿，支持按状态筛选。 */
    PageResult<PostResp> pageModerationPosts(Long page, Long size, Integer status);

    /**
     * 查询封禁作者发布过的帖子。
     */
    PageResult<PostResp> pagePostsByBannedAuthors(BannedAuthorPostQueryReq req);

    /**
     * 查询当前登录用户自己的帖子。
     */
    PageResult<PostResp> pageMyPosts(Long page, Long size);

    /** 查询当前用户按状态筛选的发布管理数据。 */
    PostManagePageResp pageMyPostManagement(String status, Long page, Long size);

    /** 查询当前用户可编辑的任意状态帖子。 */
    PostManageItemResp getMyManagedPost(Long postId);

    /** 更新当前用户已提交的帖子；非草稿自动公开，封禁状态保持不变。 */
    PostManageItemResp updateMyManagedPost(Long postId, PostPublishReq req);

    /** 删除当前用户任意状态的帖子。 */
    void deleteMyManagedPost(Long postId);

    /**
     * 查询当前用户关注对象发布的公开帖子。
     */
    PageResult<PostResp> pageFollowingPosts(Long page, Long size);

    /**
     * 查询帖子详情并增加浏览数。
     */
    PostResp getPostDetail(Long postId);

    /**
     * 创建帖子，默认自动公开。
     */
    String createPost(PostCreateReq req);

    String publishPost(PostPublishReq req);

    PageResult<PostDraftResp> pageMyDrafts(Long page, Long size);

    PostDraftResp getMyDraft(Long postId);

    PostDraftResp saveDraft(PostDraftSaveReq req);

    PostDraftResp updateDraft(Long postId, PostDraftSaveReq req);

    void deleteDraft(Long postId);

    String publishDraft(Long postId, PostPublishReq req);

    List<PostTopicResp> searchTopics(String keyword);

    List<PostCollectionResp> listMyCollections();

    PostCollectionResp createMyCollection(String name);

    void publishDuePosts();
}
