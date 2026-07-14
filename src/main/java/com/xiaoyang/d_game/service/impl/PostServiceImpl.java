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
import com.xiaoyang.d_game.common.enums.PostRankingTypeEnum;
import com.xiaoyang.d_game.dto.PostCreateReq;
import com.xiaoyang.d_game.dto.PostQueryReq;
import com.xiaoyang.d_game.dto.PostRankingQueryReq;
import com.xiaoyang.d_game.dto.PostResp;
import com.xiaoyang.d_game.entity.Board;
import com.xiaoyang.d_game.entity.Game;
import com.xiaoyang.d_game.entity.Post;
import com.xiaoyang.d_game.entity.User;
import com.xiaoyang.d_game.entity.UserFollow;
import com.xiaoyang.d_game.mapper.BoardMapper;
import com.xiaoyang.d_game.mapper.GameMapper;
import com.xiaoyang.d_game.mapper.PostMapper;
import com.xiaoyang.d_game.mapper.UserMapper;
import com.xiaoyang.d_game.mapper.UserFollowMapper;
import com.xiaoyang.d_game.security.UserContext;
import com.xiaoyang.d_game.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

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

    private final BoardMapper boardMapper;
    private final GameMapper gameMapper;
    private final UserMapper userMapper;
    private final UserFollowMapper userFollowMapper;
    private final HtmlSanitizer htmlSanitizer;

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
        if (req.getGameId() != null) {
            wrapper.eq(Post::getGameId, req.getGameId());
        }
        if (StringUtils.hasText(req.getKeyword())) {
            wrapper.like(Post::getTitle, req.getKeyword());
        }
        wrapper.orderByDesc(Post::getGmtCreate);
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
        Long userId = UserContext.getUserId();
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
     * 查询关注用户的公开帖子。
     */
    public PageResult<PostResp> pageFollowingPosts(Long page, Long size) {
        Long userId = UserContext.getUserId();
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
            Long currentUserId = UserContext.getUserId();
            UserContext context = UserContext.get();
            boolean isAuthor = currentUserId != null && Objects.equals(currentUserId, post.getUserId());
            boolean isAdmin = context != null && context.hasRole("ADMIN");
            // 未通过审核的内容不对外公开，避免普通用户绕过列表直接访问详情。
            if (!isAuthor && !isAdmin) {
                throw new BizException(ResultCode.FORBIDDEN, "帖子未通过审核");
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
     * <p>新帖默认进入待审核状态，正文先经过富文本清洗再入库。</p>
     */
    public String createPost(PostCreateReq req) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        log.debug("开始创建帖子, userId={}, boardId={}, gameId={}, title={}",
                userId, req.getBoardId(), req.getGameId(), req.getTitle());
        Board board = boardMapper.selectById(req.getBoardId());
        if (board == null) {
            throw new BizException(ResultCode.BOARD_NOT_FOUND);
        }
        if (req.getGameId() != null && gameMapper.selectById(req.getGameId()) == null) {
            throw new BizException(ResultCode.GAME_NOT_FOUND);
        }
        // 帖子不直接公开，避免垃圾内容绕过审核进入社区首页。
        Post post = new Post();
        post.setBoardId(req.getBoardId());
        post.setGameId(req.getGameId());
        post.setUserId(userId);
        post.setTitle(req.getTitle());
        post.setContent(htmlSanitizer.sanitizePostContent(req.getContent()));
        post.setStatus(ContentStatusEnum.PENDING.getCode());
        save(post);
        log.debug("创建帖子成功, postId={}, userId={}, status={}", post.getId(), userId, post.getStatus());
        return String.valueOf(post.getId());
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
        Board board = boardMapper.selectById(post.getBoardId());
        resp.setBoardName(board == null ? null : board.getName());
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
