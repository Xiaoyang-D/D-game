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
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements PostService {

    private final BoardMapper boardMapper;
    private final GameMapper gameMapper;
    private final UserMapper userMapper;
    private final UserFollowMapper userFollowMapper;
    private final HtmlSanitizer htmlSanitizer;

    @Override
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
    public PageResult<PostResp> pageFollowingPosts(Long page, Long size) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        List<Long> followeeIds = userFollowMapper.selectList(new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowerId, userId))
                .stream().map(UserFollow::getFolloweeId).toList();
        if (followeeIds.isEmpty()) {
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
            if (!isAuthor && !isAdmin) {
                throw new BizException(ResultCode.FORBIDDEN, "帖子未通过审核");
            }
        }
        getBaseMapper().update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, postId)
                .setSql("view_count = view_count + 1"));
        post.setViewCount(post.getViewCount() + 1);
        log.debug("查询帖子详情完成, postId={}, viewCount={}", postId, post.getViewCount());
        return toPostResp(post);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
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

    private PostResp toPostResp(Post post) {
        PostResp resp = new PostResp();
        resp.setId(post.getId());
        resp.setBoardId(post.getBoardId());
        resp.setGameId(post.getGameId());
        resp.setUserId(post.getUserId());
        resp.setTitle(post.getTitle());
        // Also sanitize legacy records that were saved before sanitization was introduced.
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

    private PageResult<PostResp> emptyPageResult(Long page, Long size) {
        PageResult<PostResp> pageResult = new PageResult<>();
        pageResult.setPage(page);
        pageResult.setSize(size);
        pageResult.setTotal(0L);
        pageResult.setRecords(Collections.emptyList());
        return pageResult;
    }
}
