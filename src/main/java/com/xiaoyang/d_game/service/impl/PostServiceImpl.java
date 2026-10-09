package com.xiaoyang.d_game.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaoyang.d_game.common.BizException;
import com.xiaoyang.d_game.common.PageResult;
import com.xiaoyang.d_game.common.ResultCode;
import com.xiaoyang.d_game.common.HtmlSanitizer;
import com.xiaoyang.d_game.common.enums.ContentStatusEnum;
import com.xiaoyang.d_game.common.enums.UserStatusEnum;
import com.xiaoyang.d_game.dto.BannedAuthorPostQueryReq;
import com.xiaoyang.d_game.dto.PostCollectionResp;
import com.xiaoyang.d_game.common.enums.PostRankingTypeEnum;
import com.xiaoyang.d_game.dto.PostCreateReq;
import com.xiaoyang.d_game.dto.PostDraftResp;
import com.xiaoyang.d_game.dto.PostDraftSaveReq;
import com.xiaoyang.d_game.dto.PostManageItemResp;
import com.xiaoyang.d_game.dto.PostManagePageResp;
import com.xiaoyang.d_game.dto.PostQueryReq;
import com.xiaoyang.d_game.dto.PostRankingQueryReq;
import com.xiaoyang.d_game.dto.PostResp;
import com.xiaoyang.d_game.dto.PostPublishReq;
import com.xiaoyang.d_game.dto.PostTopicResp;
import com.xiaoyang.d_game.entity.Board;
import com.xiaoyang.d_game.entity.Game;
import com.xiaoyang.d_game.entity.Post;
import com.xiaoyang.d_game.entity.PostCollection;
import com.xiaoyang.d_game.entity.PostTopic;
import com.xiaoyang.d_game.entity.PostTopicRel;
import com.xiaoyang.d_game.entity.User;
import com.xiaoyang.d_game.entity.UserFollow;
import com.xiaoyang.d_game.mapper.BoardMapper;
import com.xiaoyang.d_game.mapper.GameMapper;
import com.xiaoyang.d_game.mapper.PostMapper;
import com.xiaoyang.d_game.mapper.PostCollectionMapper;
import com.xiaoyang.d_game.mapper.PostTopicMapper;
import com.xiaoyang.d_game.mapper.PostTopicRelMapper;
import com.xiaoyang.d_game.mapper.UserMapper;
import com.xiaoyang.d_game.mapper.UserFollowMapper;
import com.xiaoyang.d_game.security.CurrentUser;
import com.xiaoyang.d_game.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@Slf4j
@RequiredArgsConstructor
/**
 * 帖子业务实现。
 *
 * <p>负责公开列表、排行榜、关注流、详情浏览、发帖和后台审核辅助查询。
 * 帖子内容会在入库和返回时都经过 HTML 清洗，兼顾新增内容和历史存量内容的安全。</p>
 */
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements PostService {

    private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");

    private final BoardMapper boardMapper;
    private final GameMapper gameMapper;
    private final UserMapper userMapper;
    private final UserFollowMapper userFollowMapper;
    private final HtmlSanitizer htmlSanitizer;
    private final PostTopicMapper postTopicMapper;
    private final PostTopicRelMapper postTopicRelMapper;
    private final PostCollectionMapper postCollectionMapper;

    @Override
    /**
     * 分页查询公开帖子。
     *
     * <p>只查询审核通过的帖子，避免待审核或被拒绝内容出现在公共列表。</p>
     */
    public PageResult<PostResp> pagePosts(PostQueryReq req) {
        log.debug("分页查询帖子, boardId={}, gameId={}, keyword={}, page={}, size={}",
                req.getBoardId(), req.getGameId(), req.getKeyword(), req.getPage(), req.getSize());
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getStatus, ContentStatusEnum.APPROVED.getCode());
        if (req.getBoardId() != null) {
            wrapper.eq(Post::getBoardId, req.getBoardId());
        }
        if (req.getAuthorId() != null) {
            User author = userMapper.selectById(req.getAuthorId());
            Long currentUserId = CurrentUser.getOptionalUserId();
            boolean isAuthor = currentUserId != null && Objects.equals(currentUserId, req.getAuthorId());
            boolean isAdmin = CurrentUser.hasRole("ADMIN");
            if (author == null || (Objects.equals(author.getStatus(), UserStatusEnum.BANNED.getCode())
                    && !isAuthor && !isAdmin)) {
                return emptyPageResult(req.getPage(), req.getSize());
            }
            wrapper.eq(Post::getUserId, req.getAuthorId());
        }
        if (req.getGameId() != null) {
            wrapper.eq(Post::getGameId, req.getGameId());
        }
        if (StringUtils.hasText(req.getKeyword())) {
            wrapper.like(Post::getTitle, req.getKeyword());
        }
        if (Boolean.TRUE.equals(req.getRecommended())) {
            wrapper.orderByDesc(Post::getLikeCount);
        }
        wrapper.orderByDesc(Post::getGmtCreate).orderByDesc(Post::getId);
        Page<Post> page = page(new Page<>(req.getPage(), req.getSize()), wrapper);
        List<PostResp> records = page.getRecords().stream().map(this::toPostResp).toList();
        PageResult<PostResp> result = new PageResult<>();
        result.setPage(page.getCurrent());
        result.setSize(page.getSize());
        result.setTotal(page.getTotal());
        result.setRecords(records);
        log.debug("分页查询帖子完成, total={}, records={}", page.getTotal(), records.size());
        return result;
    }

    @Override
    /**
     * 查询帖子排行榜。
     *
     * <p>latest 按发布时间排序，hot 使用浏览、点赞、评论、收藏的加权分数排序。</p>
     */
    public PageResult<PostResp> pageRanking(PostRankingQueryReq req) {
        PostRankingTypeEnum rankingType = PostRankingTypeEnum.fromCode(req.getType());
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getStatus, ContentStatusEnum.APPROVED.getCode());
        if (rankingType == PostRankingTypeEnum.LATEST) {
            wrapper.orderByDesc(Post::getGmtCreate);
        } else {
            wrapper.last("ORDER BY (view_count + like_count * 3 + comment_count * 5 + favorite_count * 2) DESC");
        }
        Page<Post> page = page(new Page<>(req.getPage(), req.getSize()), wrapper);
        List<PostResp> records = page.getRecords().stream().map(this::toPostResp).toList();
        PageResult<PostResp> result = new PageResult<>();
        result.setPage(page.getCurrent());
        result.setSize(page.getSize());
        result.setTotal(page.getTotal());
        result.setRecords(records);
        return result;
    }

    @Override
    /**
     * 查询待审核帖子。
     */
    public PageResult<PostResp> pageModerationPosts(Long page, Long size, Integer status) {
        if (status != null && !List.of(ContentStatusEnum.PENDING.getCode(),
                ContentStatusEnum.APPROVED.getCode(), ContentStatusEnum.REJECTED.getCode()).contains(status)) {
            throw new BizException(ResultCode.BAD_REQUEST, "无效的帖子状态");
        }
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .in(Post::getStatus, ContentStatusEnum.PENDING.getCode(),
                        ContentStatusEnum.APPROVED.getCode(), ContentStatusEnum.REJECTED.getCode())
                .eq(status != null, Post::getStatus, status)
                .orderByDesc(Post::getGmtCreate).orderByDesc(Post::getId);
        Page<Post> result = page(new Page<>(page == null || page < 1 ? 1 : page,
                size == null || size < 1 ? 10 : Math.min(size, 50)), wrapper);
        PageResult<PostResp> response = new PageResult<>();
        response.setPage(result.getCurrent());
        response.setSize(result.getSize());
        response.setTotal(result.getTotal());
        response.setRecords(result.getRecords().stream().map(this::toPostResp).toList());
        return response;
    }

    @Override
    public PageResult<PostResp> pagePendingPosts(Long page, Long size) {
        Page<Post> result = page(new Page<>(page, size), new LambdaQueryWrapper<Post>()
                .eq(Post::getStatus, ContentStatusEnum.PENDING.getCode())
                .orderByAsc(Post::getGmtCreate));
        PageResult<PostResp> pageResult = new PageResult<>();
        pageResult.setPage(result.getCurrent());
        pageResult.setSize(result.getSize());
        pageResult.setTotal(result.getTotal());
        pageResult.setRecords(result.getRecords().stream().map(this::toPostResp).toList());
        return pageResult;
    }

    @Override
    /**
     * 查询封禁作者发布过的帖子。
     *
     * <p>先筛出封禁用户 ID，再按这些用户 ID 查询帖子；没有匹配用户时直接返回空分页。</p>
     */
    public PageResult<PostResp> pagePostsByBannedAuthors(BannedAuthorPostQueryReq req) {
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(User::getStatus, UserStatusEnum.BANNED.getCode());
        if (req.getAuthorId() != null) {
            userWrapper.eq(User::getId, req.getAuthorId());
        }
        if (StringUtils.hasText(req.getKeyword())) {
            userWrapper.and(w -> w.like(User::getNickname, req.getKeyword())
                    .or()
                    .like(User::getUsername, req.getKeyword()));
        }
        List<Long> bannedUserIds = userMapper.selectList(userWrapper).stream()
                .map(User::getId)
                .toList();
        if (bannedUserIds.isEmpty()) {
            return emptyPageResult(req.getPage(), req.getSize());
        }
        Page<Post> result = page(new Page<>(req.getPage(), req.getSize()), new LambdaQueryWrapper<Post>()
                .in(Post::getUserId, bannedUserIds)
                .orderByDesc(Post::getGmtCreate));
        PageResult<PostResp> pageResult = new PageResult<>();
        pageResult.setPage(result.getCurrent());
        pageResult.setSize(result.getSize());
        pageResult.setTotal(result.getTotal());
        pageResult.setRecords(result.getRecords().stream().map(this::toPostResp).toList());
        return pageResult;
    }

    @Override
    /**
     * 查询当前用户自己的帖子。
     */
    public PageResult<PostResp> pageMyPosts(Long page, Long size) {
        Long userId = CurrentUser.getUserId();
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        Page<Post> result = page(new Page<>(page, size), new LambdaQueryWrapper<Post>()
                .eq(Post::getUserId, userId)
                .orderByDesc(Post::getGmtCreate));
        PageResult<PostResp> pageResult = new PageResult<>();
        pageResult.setPage(result.getCurrent());
        pageResult.setSize(result.getSize());
        pageResult.setTotal(result.getTotal());
        pageResult.setRecords(result.getRecords().stream().map(this::toPostResp).toList());
        return pageResult;
    }

    @Override
    /**
     * 查询当前用户的发布管理列表。
     *
     * <p>列表只返回指定审核状态，统计数量则覆盖四种状态，保证 Tab 切换时数字稳定。</p>
     */
    public PostManagePageResp pageMyPostManagement(String status, Long page, Long size) {
        Long userId = currentUserId();
        ContentStatusEnum contentStatus = resolveManageStatus(status);
        long safePage = page == null || page < 1 ? 1 : page;
        long safeSize = size == null || size < 1 ? 10 : Math.min(size, 50);

        Page<Post> result = page(new Page<>(safePage, safeSize), new LambdaQueryWrapper<Post>()
                .eq(Post::getUserId, userId)
                .eq(Post::getStatus, contentStatus.getCode())
                .orderByDesc(Post::getGmtModified));
        PageResult<PostManageItemResp> pageResult = new PageResult<>();
        pageResult.setPage(result.getCurrent());
        pageResult.setSize(result.getSize());
        pageResult.setTotal(result.getTotal());
        pageResult.setRecords(result.getRecords().stream().map(this::toManageItemResp).toList());

        PostManagePageResp response = PostManagePageResp.from(pageResult);
        response.setPublishedCount(countMyPostsByStatus(userId, ContentStatusEnum.APPROVED));
        response.setPendingCount(countMyPostsByStatus(userId, ContentStatusEnum.PENDING));
        response.setRejectedCount(countMyPostsByStatus(userId, ContentStatusEnum.REJECTED));
        response.setDraftCount(countMyPostsByStatus(userId, ContentStatusEnum.DRAFT));
        return response;
    }

    @Override
    /** 查询当前用户自己的帖子详情，不向其他用户暴露管理数据。 */
    public PostManageItemResp getMyManagedPost(Long postId) {
        return toManageItemResp(getRequiredManagedPost(postId, currentUserId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    /**
     * 更新管理页帖子。
     *
     * <p>草稿保持草稿状态；已发布和历史待审核内容修改后自动公开；封禁内容继续保持封禁。</p>
     */
    public PostManageItemResp updateMyManagedPost(Long postId, PostPublishReq req) {
        Long userId = currentUserId();
        Post post = getRequiredManagedPost(postId, userId);
        Integer originalStatus = post.getStatus();
        validatePublishRequest(req, userId);
        applyPublishFields(post, req, userId);
        if (Objects.equals(post.getStatus(), ContentStatusEnum.DRAFT.getCode())) {
            post.setStatus(ContentStatusEnum.DRAFT.getCode());
        } else {
            // 管理员封禁的帖子修改后仍保持封禁，作者不能绕过管理操作。
            if (!Objects.equals(post.getStatus(), ContentStatusEnum.REJECTED.getCode())) {
                post.setStatus(ContentStatusEnum.APPROVED.getCode());
            }
            post.setScheduledPublishAt(null);
        }
        boolean updated = update(post, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, postId).eq(Post::getUserId, userId)
                .eq(Post::getStatus, originalStatus));
        if (!updated) {
            throw new BizException(ResultCode.BAD_REQUEST, "帖子状态已变化，请刷新后重试");
        }
        replaceTopics(post.getId(), req.getTopicNames());
        return toManageItemResp(post);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    /** 删除当前用户任意状态的帖子，并由 MyBatis-Plus 执行逻辑删除。 */
    public void deleteMyManagedPost(Long postId) {
        Post post = getRequiredManagedPost(postId, currentUserId());
        removeById(post.getId());
    }

    @Override
    /**
     * 查询关注用户的公开帖子。
     */
    public PageResult<PostResp> pageFollowingPosts(Long page, Long size) {
        Long userId = CurrentUser.getUserId();
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        List<Long> followeeIds = userFollowMapper.selectList(new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowerId, userId))
                .stream().map(UserFollow::getFolloweeId).toList();
        if (followeeIds.isEmpty()) {
            // 没有关注任何人时直接返回空页，避免生成空 in 条件 SQL。
            return emptyPageResult(page, size);
        }
        Page<Post> result = page(new Page<>(page, size), new LambdaQueryWrapper<Post>()
                .in(Post::getUserId, followeeIds)
                .eq(Post::getStatus, ContentStatusEnum.APPROVED.getCode())
                .orderByDesc(Post::getGmtCreate));
        PageResult<PostResp> pageResult = new PageResult<>();
        pageResult.setPage(result.getCurrent());
        pageResult.setSize(result.getSize());
        pageResult.setTotal(result.getTotal());
        pageResult.setRecords(result.getRecords().stream().map(this::toPostResp).toList());
        return pageResult;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    /**
     * 查询帖子详情并增加浏览数。
     *
     * <p>非公开状态的帖子只有作者本人和管理员可见；浏览数使用 SQL 原子自增，减少并发覆盖。</p>
     */
    public PostResp getPostDetail(Long postId) {
        log.debug("查询帖子详情, postId={}", postId);
        Post post = getById(postId);
        if (post == null) {
            throw new BizException(ResultCode.POST_NOT_FOUND);
        }
        if (!Objects.equals(post.getStatus(), ContentStatusEnum.APPROVED.getCode())) {
            Long currentUserId = CurrentUser.getOptionalUserId();
            boolean isAuthor = currentUserId != null && Objects.equals(currentUserId, post.getUserId());
            boolean isAdmin = CurrentUser.hasRole("ADMIN");
            // 未通过审核的内容不对外公开，避免普通用户绕过列表直接访问详情。
            if (!isAuthor && !isAdmin) {
                throw new BizException(ResultCode.FORBIDDEN, "帖子未公开或已被封禁");
            }
        }
        // 数据库原子自增真实浏览数，再同步内存对象，保证返回值也是最新浏览数。
        getBaseMapper().update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, postId)
                .setSql("view_count = view_count + 1"));
        post.setViewCount(post.getViewCount() + 1);
        log.debug("查询帖子详情完成, postId={}, viewCount={}", postId, post.getViewCount());
        return toPostResp(post);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    /**
     * 创建帖子。
     *
     * <p>新帖默认自动通过并公开展示，正文先经过富文本清洗再入库。</p>
     */
    public String createPost(PostCreateReq req) {
        Long userId = CurrentUser.getUserId();
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        log.debug("开始创建帖子, userId={}, boardId={}, gameId={}, title={}",
                userId, req.getBoardId(), req.getGameId(), req.getTitle());
        Board board = boardMapper.selectById(req.getBoardId());
        if (board == null) {
            throw new BizException(ResultCode.BOARD_NOT_FOUND);
        }
        validateOfficialBoard(board);
        if (req.getGameId() != null && gameMapper.selectById(req.getGameId()) == null) {
            throw new BizException(ResultCode.GAME_NOT_FOUND);
        }
        // 帖子自动通过，违规内容由管理员事后封禁。
        Post post = new Post();
        post.setBoardId(req.getBoardId());
        post.setGameId(req.getGameId());
        post.setUserId(userId);
        post.setTitle(req.getTitle());
        post.setContent(htmlSanitizer.sanitizePostContent(req.getContent()));
        post.setStatus(ContentStatusEnum.APPROVED.getCode());
        save(post);
        log.debug("创建帖子成功, postId={}, userId={}, status={}", post.getId(), userId, post.getStatus());
        return String.valueOf(post.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String publishPost(PostPublishReq req) {
        Long userId = currentUserId();
        validatePublishRequest(req, userId);
        Post post = new Post();
        applyPublishFields(post, req, userId);
        post.setStatus(req.getScheduledPublishAt() == null
                ? ContentStatusEnum.APPROVED.getCode()
                : ContentStatusEnum.DRAFT.getCode());
        save(post);
        replaceTopics(post.getId(), req.getTopicNames());
        return String.valueOf(post.getId());
    }

    @Override
    public PageResult<PostDraftResp> pageMyDrafts(Long page, Long size) {
        Long userId = currentUserId();
        Page<Post> result = page(new Page<>(page == null || page < 1 ? 1 : page, size == null || size < 1 ? 10 : size),
                new LambdaQueryWrapper<Post>()
                        .eq(Post::getUserId, userId)
                        .eq(Post::getStatus, ContentStatusEnum.DRAFT.getCode())
                        .orderByDesc(Post::getGmtModified));
        PageResult<PostDraftResp> response = new PageResult<>();
        response.setPage(result.getCurrent());
        response.setSize(result.getSize());
        response.setTotal(result.getTotal());
        response.setRecords(result.getRecords().stream().map(this::toDraftResp).toList());
        return response;
    }

    @Override
    public PostDraftResp getMyDraft(Long postId) {
        return toDraftResp(getRequiredDraft(postId, currentUserId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PostDraftResp saveDraft(PostDraftSaveReq req) {
        Long userId = currentUserId();
        validateDraftFields(req);
        validateCollectionOwner(req.getCollectionId(), userId);
        if (req.getScheduledPublishAt() != null) {
            validateScheduledDraft(req, userId);
        }
        Post post = new Post();
        post.setUserId(userId);
        applyDraftFields(post, req);
        post.setStatus(ContentStatusEnum.DRAFT.getCode());
        save(post);
        replaceTopics(post.getId(), req.getTopicNames());
        return toDraftResp(post);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PostDraftResp updateDraft(Long postId, PostDraftSaveReq req) {
        Long userId = currentUserId();
        Post post = getRequiredDraft(postId, userId);
        validateDraftFields(req);
        validateCollectionOwner(req.getCollectionId(), userId);
        if (req.getScheduledPublishAt() != null) {
            validateScheduledDraft(req, userId);
        }
        applyDraftFields(post, req);
        updateById(post);
        replaceTopics(post.getId(), req.getTopicNames());
        return toDraftResp(post);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDraft(Long postId) {
        Post post = getRequiredDraft(postId, currentUserId());
        removeById(post.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String publishDraft(Long postId, PostPublishReq req) {
        Long userId = currentUserId();
        Post post = getRequiredDraft(postId, userId);
        validatePublishRequest(req, userId);
        applyPublishFields(post, req, userId);
        post.setStatus(req.getScheduledPublishAt() == null
                ? ContentStatusEnum.APPROVED.getCode()
                : ContentStatusEnum.DRAFT.getCode());
        updateById(post);
        replaceTopics(post.getId(), req.getTopicNames());
        return String.valueOf(post.getId());
    }

    @Override
    public List<PostTopicResp> searchTopics(String keyword) {
        LambdaQueryWrapper<PostTopic> wrapper = new LambdaQueryWrapper<PostTopic>()
                .orderByAsc(PostTopic::getName)
                .last("LIMIT 20");
        if (StringUtils.hasText(keyword)) {
            wrapper.like(PostTopic::getName, keyword.trim());
        }
        return postTopicMapper.selectList(wrapper).stream().map(this::toTopicResp).toList();
    }

    @Override
    public List<PostCollectionResp> listMyCollections() {
        Long userId = currentUserId();
        return postCollectionMapper.selectList(new LambdaQueryWrapper<PostCollection>()
                        .eq(PostCollection::getUserId, userId)
                        .orderByDesc(PostCollection::getGmtModified))
                .stream().map(this::toCollectionResp).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PostCollectionResp createMyCollection(String name) {
        Long userId = currentUserId();
        if (!StringUtils.hasText(name) || name.trim().length() > 64) {
            throw new BizException(ResultCode.BAD_REQUEST, "合集名称不能为空且不能超过64个字符");
        }
        String normalizedName = name.trim();
        if (postCollectionMapper.selectCount(new LambdaQueryWrapper<PostCollection>()
                .eq(PostCollection::getUserId, userId)
                .eq(PostCollection::getName, normalizedName)) > 0) {
            throw new BizException(ResultCode.CONFLICT, "合集名称已存在");
        }
        PostCollection collection = new PostCollection();
        collection.setUserId(userId);
        collection.setName(normalizedName);
        postCollectionMapper.insert(collection);
        return toCollectionResp(collection);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishDuePosts() {
        LocalDateTime now = LocalDateTime.now(SHANGHAI_ZONE);
        List<Post> duePosts = list(new LambdaQueryWrapper<Post>()
                .eq(Post::getStatus, ContentStatusEnum.DRAFT.getCode())
                .isNotNull(Post::getScheduledPublishAt)
                .le(Post::getScheduledPublishAt, now)
                .last("LIMIT 100"));
        for (Post post : duePosts) {
            update(new LambdaUpdateWrapper<Post>()
                    .eq(Post::getId, post.getId())
                    .eq(Post::getStatus, ContentStatusEnum.DRAFT.getCode())
                    .set(Post::getStatus, ContentStatusEnum.APPROVED.getCode())
                    .set(Post::getScheduledPublishAt, null));
        }
    }

    private Long currentUserId() {
        Long userId = CurrentUser.getUserId();
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return userId;
    }

    private Post getRequiredDraft(Long postId, Long userId) {
        Post post = getById(postId);
        if (post == null || !Objects.equals(post.getUserId(), userId)
                || !Objects.equals(post.getStatus(), ContentStatusEnum.DRAFT.getCode())) {
            throw new BizException(ResultCode.POST_NOT_FOUND);
        }
        return post;
    }

    private Post getRequiredManagedPost(Long postId, Long userId) {
        Post post = getById(postId);
        if (post == null || !Objects.equals(post.getUserId(), userId)) {
            // Do not reveal another user's private draft or management metadata.
            throw new BizException(ResultCode.POST_NOT_FOUND);
        }
        return post;
    }

    private ContentStatusEnum resolveManageStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return ContentStatusEnum.APPROVED;
        }
        for (ContentStatusEnum value : ContentStatusEnum.values()) {
            if (value.name().equalsIgnoreCase(status.trim())) {
                return value;
            }
        }
        throw new BizException(ResultCode.BAD_REQUEST, "无效的发布管理状态");
    }

    private long countMyPostsByStatus(Long userId, ContentStatusEnum status) {
        return count(new LambdaQueryWrapper<Post>()
                .eq(Post::getUserId, userId)
                .eq(Post::getStatus, status.getCode()));
    }

    private void validateDraftFields(PostDraftSaveReq req) {
        if (req.getBoardId() != null) {
            Board board = boardMapper.selectById(req.getBoardId());
            if (board == null) {
                throw new BizException(ResultCode.BOARD_NOT_FOUND);
            }
            validateOfficialBoard(board);
        }
        if (req.getTitle() != null && req.getTitle().length() > 40) {
            throw new BizException(ResultCode.BAD_REQUEST, "标题长度不能超过40");
        }
        if (req.getContent() != null && req.getContent().length() > 20000) {
            throw new BizException(ResultCode.BAD_REQUEST, "正文长度不能超过20000");
        }
        normalizeTopicNames(req.getTopicNames());
    }

    private void validatePublishRequest(PostPublishReq req, Long userId) {
        if (req.getBoardId() == null || !StringUtils.hasText(req.getTitle()) || !StringUtils.hasText(req.getContent())) {
            throw new BizException(ResultCode.BAD_REQUEST, "版块、标题和正文不能为空");
        }
        if (req.getTitle().trim().length() > 40 || req.getContent().length() > 20000) {
            throw new BizException(ResultCode.BAD_REQUEST, "标题或正文超过长度限制");
        }
        Board board = boardMapper.selectById(req.getBoardId());
        if (board == null) {
            throw new BizException(ResultCode.BOARD_NOT_FOUND);
        }
        validateOfficialBoard(board);
        if (req.getGameId() != null && gameMapper.selectById(req.getGameId()) == null) {
            throw new BizException(ResultCode.GAME_NOT_FOUND);
        }
        validateCollectionOwner(req.getCollectionId(), userId);
        normalizeTopicNames(req.getTopicNames());
        if (req.getScheduledPublishAt() != null) {
            validateSchedule(req.getScheduledPublishAt());
        }
    }

    /**
     * A scheduled draft is already a publishable submission. Keep ordinary drafts
     * permissive, but require all publish fields before accepting a schedule.
     */
    private void validateScheduledDraft(PostDraftSaveReq req, Long userId) {
        if (req.getBoardId() == null || !StringUtils.hasText(req.getTitle())
                || !StringUtils.hasText(req.getContent())) {
            throw new BizException(ResultCode.BAD_REQUEST, "定时发布需要版块、标题和正文");
        }
        if (req.getTitle().trim().length() > 40 || req.getContent().length() > 20000) {
            throw new BizException(ResultCode.BAD_REQUEST, "标题或正文超过长度限制");
        }
        Board board = boardMapper.selectById(req.getBoardId());
        if (board == null) {
            throw new BizException(ResultCode.BOARD_NOT_FOUND);
        }
        validateOfficialBoard(board);
        if (req.getGameId() != null && gameMapper.selectById(req.getGameId()) == null) {
            throw new BizException(ResultCode.GAME_NOT_FOUND);
        }
        validateCollectionOwner(req.getCollectionId(), userId);
        normalizeTopicNames(req.getTopicNames());
        validateSchedule(req.getScheduledPublishAt());
    }

    private void validateOfficialBoard(Board board) {
        if ("官方".equals(board.getName()) && !CurrentUser.hasRole("ADMIN")) {
            throw new BizException(ResultCode.FORBIDDEN, "官方分区仅管理员可以发布");
        }
    }

    private void validateSchedule(LocalDateTime scheduledAt) {
        LocalDateTime now = LocalDateTime.now(SHANGHAI_ZONE);
        if (scheduledAt.isBefore(now.plusHours(2)) || scheduledAt.isAfter(now.plusDays(15))) {
            throw new BizException(ResultCode.BAD_REQUEST, "定时发布时间必须在当前时间后2小时至15天内");
        }
    }

    private void validateCollectionOwner(Long collectionId, Long userId) {
        if (collectionId == null) {
            return;
        }
        PostCollection collection = postCollectionMapper.selectOne(new LambdaQueryWrapper<PostCollection>()
                .eq(PostCollection::getId, collectionId)
                .eq(PostCollection::getUserId, userId)
                .last("LIMIT 1"));
        if (collection == null) {
            throw new BizException(ResultCode.FORBIDDEN, "无权使用该合集");
        }
    }

    private void applyDraftFields(Post post, PostDraftSaveReq req) {
        post.setBoardId(req.getBoardId());
        post.setGameId(req.getGameId());
        post.setCollectionId(req.getCollectionId());
        post.setTitle(req.getTitle() == null ? "" : req.getTitle().trim());
        post.setContent(htmlSanitizer.sanitizePostContent(req.getContent() == null ? "" : req.getContent()));
        post.setIsOriginal(Boolean.TRUE.equals(req.getIsOriginal()));
        post.setContainsAiGenerated(Boolean.TRUE.equals(req.getContainsAiGenerated()));
        post.setScheduledPublishAt(req.getScheduledPublishAt());
    }

    private void applyPublishFields(Post post, PostPublishReq req, Long userId) {
        post.setUserId(userId);
        post.setBoardId(req.getBoardId());
        post.setGameId(req.getGameId());
        post.setCollectionId(req.getCollectionId());
        post.setTitle(req.getTitle().trim());
        post.setContent(htmlSanitizer.sanitizePostContent(req.getContent()));
        post.setIsOriginal(Boolean.TRUE.equals(req.getIsOriginal()));
        post.setContainsAiGenerated(Boolean.TRUE.equals(req.getContainsAiGenerated()));
        post.setScheduledPublishAt(req.getScheduledPublishAt());
    }

    private List<String> normalizeTopicNames(List<String> names) {
        if (names == null || names.isEmpty()) {
            return List.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String name : names) {
            if (!StringUtils.hasText(name)) {
                continue;
            }
            String value = name.trim();
            if (value.startsWith("#")) {
                value = value.substring(1).trim();
            }
            if (value.length() > 64) {
                throw new BizException(ResultCode.BAD_REQUEST, "话题长度不能超过64个字符");
            }
            if (!value.isEmpty()) {
                normalized.add(value);
            }
        }
        if (normalized.size() > 5) {
            throw new BizException(ResultCode.BAD_REQUEST, "话题最多选择5个");
        }
        return new ArrayList<>(normalized);
    }

    private void replaceTopics(Long postId, List<String> names) {
        postTopicRelMapper.deletePhysicallyByPostId(postId);
        for (String name : normalizeTopicNames(names)) {
            PostTopic topic = postTopicMapper.selectOne(new LambdaQueryWrapper<PostTopic>()
                    .eq(PostTopic::getName, name)
                    .last("LIMIT 1"));
            if (topic == null) {
                topic = new PostTopic();
                topic.setName(name);
                postTopicMapper.insert(topic);
            }
            PostTopicRel relation = new PostTopicRel();
            relation.setPostId(postId);
            relation.setTopicId(topic.getId());
            postTopicRelMapper.insert(relation);
        }
    }

    private List<PostTopicResp> loadTopics(Long postId) {
        List<PostTopicRel> relations = postTopicRelMapper.selectList(new LambdaQueryWrapper<PostTopicRel>()
                .eq(PostTopicRel::getPostId, postId)
                .orderByAsc(PostTopicRel::getGmtCreate));
        if (relations.isEmpty()) {
            return List.of();
        }
        Map<Long, PostTopic> topics = postTopicMapper.selectBatchIds(relations.stream()
                        .map(PostTopicRel::getTopicId).toList())
                .stream().collect(Collectors.toMap(PostTopic::getId, topic -> topic, (first, ignored) -> first));
        return relations.stream().map(PostTopicRel::getTopicId).map(topics::get)
                .filter(Objects::nonNull).map(this::toTopicResp).toList();
    }

    private PostTopicResp toTopicResp(PostTopic topic) {
        PostTopicResp response = new PostTopicResp();
        response.setId(topic.getId());
        response.setName(topic.getName());
        return response;
    }

    private PostCollectionResp toCollectionResp(PostCollection collection) {
        PostCollectionResp response = new PostCollectionResp();
        response.setId(collection.getId());
        response.setName(collection.getName());
        return response;
    }

    private PostDraftResp toDraftResp(Post post) {
        PostDraftResp response = new PostDraftResp();
        response.setId(post.getId());
        response.setBoardId(post.getBoardId());
        if (post.getBoardId() != null) {
            Board board = boardMapper.selectById(post.getBoardId());
            response.setBoardName(board == null ? null : board.getName());
        }
        response.setGameId(post.getGameId());
        if (post.getGameId() != null) {
            Game game = gameMapper.selectById(post.getGameId());
            response.setGameName(game == null ? null : game.getName());
        }
        response.setTitle(post.getTitle());
        response.setContent(htmlSanitizer.sanitizePostContent(post.getContent()));
        response.setStatus(post.getStatus());
        response.setIsOriginal(post.getIsOriginal());
        response.setContainsAiGenerated(post.getContainsAiGenerated());
        response.setScheduledPublishAt(post.getScheduledPublishAt());
        response.setCollectionId(post.getCollectionId());
        if (post.getCollectionId() != null) {
            PostCollection collection = postCollectionMapper.selectById(post.getCollectionId());
            response.setCollectionName(collection == null ? null : collection.getName());
        }
        response.setTopics(loadTopics(post.getId()));
        response.setGmtModified(post.getGmtModified());
        return response;
    }

    private PostManageItemResp toManageItemResp(Post post) {
        PostManageItemResp response = new PostManageItemResp();
        response.setId(post.getId());
        response.setBoardId(post.getBoardId());
        if (post.getBoardId() != null) {
            Board board = boardMapper.selectById(post.getBoardId());
            response.setBoardName(board == null ? null : board.getName());
        }
        response.setGameId(post.getGameId());
        if (post.getGameId() != null) {
            Game game = gameMapper.selectById(post.getGameId());
            response.setGameName(game == null ? null : game.getName());
        }
        response.setCollectionId(post.getCollectionId());
        if (post.getCollectionId() != null) {
            PostCollection collection = postCollectionMapper.selectById(post.getCollectionId());
            response.setCollectionName(collection == null ? null : collection.getName());
        }
        response.setTitle(post.getTitle());
        response.setContent(htmlSanitizer.sanitizePostContent(post.getContent()));
        response.setStatus(post.getStatus());
        response.setIsOriginal(Boolean.TRUE.equals(post.getIsOriginal()));
        response.setContainsAiGenerated(Boolean.TRUE.equals(post.getContainsAiGenerated()));
        response.setTopics(loadTopics(post.getId()));
        response.setViewCount(post.getViewCount());
        response.setLikeCount(post.getLikeCount());
        response.setCommentCount(post.getCommentCount());
        response.setFavoriteCount(post.getFavoriteCount());
        response.setScheduledPublishAt(post.getScheduledPublishAt());
        response.setGmtCreate(post.getGmtCreate());
        response.setGmtModified(post.getGmtModified());
        return response;
    }

    /**
     * 将帖子实体转换为前端响应。
     *
     * <p>这里会补充版块名、游戏名、作者信息，并再次清洗正文，兼容引入清洗逻辑前保存的历史内容。</p>
     */
    private PostResp toPostResp(Post post) {
        PostResp resp = new PostResp();
        resp.setId(post.getId());
        resp.setBoardId(post.getBoardId());
        resp.setGameId(post.getGameId());
        resp.setUserId(post.getUserId());
        resp.setTitle(post.getTitle());
        // 对引入清洗逻辑前保存的历史帖子也再次清洗，避免旧数据直接回显产生 XSS 风险。
        resp.setContent(htmlSanitizer.sanitizePostContent(post.getContent()));
        resp.setStatus(post.getStatus());
        resp.setViewCount(post.getViewCount());
        resp.setLikeCount(post.getLikeCount());
        resp.setCommentCount(post.getCommentCount());
        resp.setFavoriteCount(post.getFavoriteCount());
        resp.setGmtCreate(post.getGmtCreate());
        resp.setCollectionId(post.getCollectionId());
        if (post.getCollectionId() != null) {
            PostCollection collection = postCollectionMapper.selectById(post.getCollectionId());
            resp.setCollectionName(collection == null ? null : collection.getName());
        }
        resp.setTopics(loadTopics(post.getId()));
        resp.setOriginal(Boolean.TRUE.equals(post.getIsOriginal()));
        resp.setContainsAiGenerated(Boolean.TRUE.equals(post.getContainsAiGenerated()));
        resp.setScheduledPublishAt(post.getScheduledPublishAt());
        if (post.getBoardId() != null) {
            Board board = boardMapper.selectById(post.getBoardId());
            resp.setBoardName(board == null ? null : board.getName());
        }
        if (post.getGameId() != null) {
            Game game = gameMapper.selectById(post.getGameId());
            resp.setGameName(game == null ? null : game.getName());
        }
        User author = userMapper.selectById(post.getUserId());
        if (author != null) {
            resp.setAuthorNickname(author.getNickname());
            resp.setAuthorUsername(author.getUsername());
            resp.setAuthorAvatarUrl(author.getAvatarUrl());
            resp.setAuthorStatus(author.getStatus());
        }
        return resp;
    }

    /**
     * 构造空分页响应。
     */
    private PageResult<PostResp> emptyPageResult(Long page, Long size) {
        PageResult<PostResp> pageResult = new PageResult<>();
        pageResult.setPage(page);
        pageResult.setSize(size);
        pageResult.setTotal(0L);
        pageResult.setRecords(Collections.emptyList());
        return pageResult;
    }
}
