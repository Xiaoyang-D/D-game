package com.xiaoyang.d_game.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaoyang.d_game.common.BizException;
import com.xiaoyang.d_game.common.PageResult;
import com.xiaoyang.d_game.common.ResultCode;
import com.xiaoyang.d_game.common.enums.ContentStatusEnum;
import com.xiaoyang.d_game.common.enums.NotificationTypeEnum;
import com.xiaoyang.d_game.common.enums.TargetTypeEnum;
import com.xiaoyang.d_game.dto.CommentCreateReq;
import com.xiaoyang.d_game.dto.CommentResp;
import com.xiaoyang.d_game.entity.Comment;
import com.xiaoyang.d_game.entity.Post;
import com.xiaoyang.d_game.entity.User;
import com.xiaoyang.d_game.entity.UserFavorite;
import com.xiaoyang.d_game.entity.UserFollow;
import com.xiaoyang.d_game.entity.UserLike;
import com.xiaoyang.d_game.mapper.CommentMapper;
import com.xiaoyang.d_game.mapper.PostMapper;
import com.xiaoyang.d_game.mapper.UserFavoriteMapper;
import com.xiaoyang.d_game.mapper.UserFollowMapper;
import com.xiaoyang.d_game.mapper.UserLikeMapper;
import com.xiaoyang.d_game.mapper.UserMapper;
import com.xiaoyang.d_game.security.UserContext;
import com.xiaoyang.d_game.service.InteractService;
import com.xiaoyang.d_game.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InteractServiceImpl implements InteractService {

    private final CommentMapper commentMapper;
    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final UserLikeMapper userLikeMapper;
    private final UserFavoriteMapper userFavoriteMapper;
    private final UserFollowMapper userFollowMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createComment(CommentCreateReq req) {
        Long userId = currentUserId();
        Post post = postMapper.selectById(req.getPostId());
        if (post == null) {
            throw new BizException(ResultCode.POST_NOT_FOUND);
        }
        if (req.getParentId() != null && req.getParentId() > 0) {
            Comment parent = commentMapper.selectById(req.getParentId());
            if (parent == null) {
                throw new BizException(ResultCode.COMMENT_NOT_FOUND);
            }
        }
        Comment comment = new Comment();
        comment.setPostId(req.getPostId());
        comment.setUserId(userId);
        comment.setParentId(req.getParentId() == null ? 0L : req.getParentId());
        comment.setContent(req.getContent());
        comment.setStatus(ContentStatusEnum.APPROVED.getCode());
        commentMapper.insert(comment);

        incrementPostCounter(post.getId(), "comment_count", 1);

        notificationService.sendNotification(post.getUserId(), userId, NotificationTypeEnum.COMMENT.getCode(),
                "收到新评论", "你的帖子收到了新评论", TargetTypeEnum.POST.getCode(), post.getId());
        return comment.getId();
    }

    @Override
    public PageResult<CommentResp> pageComments(Long postId, Long page, Long size) {
        Page<Comment> result = new Page<>(page, size);
        commentMapper.selectPage(result, new LambdaQueryWrapper<Comment>()
                .eq(Comment::getPostId, postId)
                .eq(Comment::getStatus, ContentStatusEnum.APPROVED.getCode())
                .orderByAsc(Comment::getGmtCreate));
        List<Comment> records = result.getRecords();
        Map<Long, String> nickMap = loadNicknames(records.stream().map(Comment::getUserId).distinct().toList());
        List<CommentResp> respList = records.stream().map(c -> {
            CommentResp resp = new CommentResp();
            resp.setId(c.getId());
            resp.setPostId(c.getPostId());
            resp.setParentId(c.getParentId());
            resp.setUserId(c.getUserId());
            resp.setUserNickname(nickMap.get(c.getUserId()));
            resp.setContent(c.getContent());
            resp.setLikeCount(c.getLikeCount());
            resp.setGmtCreate(c.getGmtCreate());
            return resp;
        }).toList();
        PageResult<CommentResp> pageResult = new PageResult<>();
        pageResult.setPage(result.getCurrent());
        pageResult.setSize(result.getSize());
        pageResult.setTotal(result.getTotal());
        pageResult.setRecords(respList);
        return pageResult;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void like(Integer targetType, Long targetId) {
        Long userId = currentUserId();
        TargetTypeEnum type = TargetTypeEnum.of(targetType);
        UserLike existing = userLikeMapper.selectOne(new LambdaQueryWrapper<UserLike>()
                .eq(UserLike::getUserId, userId)
                .eq(UserLike::getTargetType, targetType)
                .eq(UserLike::getTargetId, targetId)
                .last("LIMIT 1"));
        if (existing != null) {
            throw new BizException(ResultCode.ALREADY_LIKED);
        }
        UserLike like = new UserLike();
        like.setUserId(userId);
        like.setTargetType(targetType);
        like.setTargetId(targetId);
        userLikeMapper.insert(like);
        adjustLikeCount(type, targetId, 1, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlike(Integer targetType, Long targetId) {
        Long userId = currentUserId();
        TargetTypeEnum type = TargetTypeEnum.of(targetType);
        UserLike existing = userLikeMapper.selectOne(new LambdaQueryWrapper<UserLike>()
                .eq(UserLike::getUserId, userId)
                .eq(UserLike::getTargetType, targetType)
                .eq(UserLike::getTargetId, targetId)
                .last("LIMIT 1"));
        if (existing == null) {
            throw new BizException(ResultCode.NOT_LIKED);
        }
        userLikeMapper.deleteById(existing.getId());
        adjustLikeCount(type, targetId, -1, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void favorite(Integer targetType, Long targetId) {
        Long userId = currentUserId();
        UserFavorite existing = userFavoriteMapper.selectOne(new LambdaQueryWrapper<UserFavorite>()
                .eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getTargetType, targetType)
                .eq(UserFavorite::getTargetId, targetId)
                .last("LIMIT 1"));
        if (existing != null) {
            throw new BizException(ResultCode.ALREADY_FAVORITED);
        }
        UserFavorite favorite = new UserFavorite();
        favorite.setUserId(userId);
        favorite.setTargetType(targetType);
        favorite.setTargetId(targetId);
        userFavoriteMapper.insert(favorite);
        if (Objects.equals(targetType, TargetTypeEnum.POST.getCode())) {
            Post post = postMapper.selectById(targetId);
            if (post != null) {
                incrementPostCounter(post.getId(), "favorite_count", 1);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfavorite(Integer targetType, Long targetId) {
        Long userId = currentUserId();
        UserFavorite existing = userFavoriteMapper.selectOne(new LambdaQueryWrapper<UserFavorite>()
                .eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getTargetType, targetType)
                .eq(UserFavorite::getTargetId, targetId)
                .last("LIMIT 1"));
        if (existing == null) {
            throw new BizException(ResultCode.NOT_FAVORITED);
        }
        userFavoriteMapper.deleteById(existing.getId());
        if (Objects.equals(targetType, TargetTypeEnum.POST.getCode())) {
            Post post = postMapper.selectById(targetId);
            if (post != null && post.getFavoriteCount() > 0) {
                incrementPostCounter(post.getId(), "favorite_count", -1);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void follow(Long followeeId) {
        Long userId = currentUserId();
        if (Objects.equals(userId, followeeId)) {
            throw new BizException(ResultCode.CANNOT_FOLLOW_SELF);
        }
        User followee = userMapper.selectById(followeeId);
        if (followee == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        UserFollow existing = userFollowMapper.selectOne(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, userId)
                .eq(UserFollow::getFolloweeId, followeeId)
                .last("LIMIT 1"));
        if (existing != null) {
            throw new BizException(ResultCode.ALREADY_FOLLOWED);
        }
        UserFollow follow = new UserFollow();
        follow.setFollowerId(userId);
        follow.setFolloweeId(followeeId);
        userFollowMapper.insert(follow);
        notificationService.sendNotification(followeeId, userId, NotificationTypeEnum.FOLLOW.getCode(),
                "新增关注", "有人关注了你", null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfollow(Long followeeId) {
        Long userId = currentUserId();
        UserFollow existing = userFollowMapper.selectOne(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, userId)
                .eq(UserFollow::getFolloweeId, followeeId)
                .last("LIMIT 1"));
        if (existing == null) {
            throw new BizException(ResultCode.NOT_FOLLOWED);
        }
        userFollowMapper.deleteById(existing.getId());
    }

    private void adjustLikeCount(TargetTypeEnum type, Long targetId, int delta, Long operatorId) {
        if (type == TargetTypeEnum.POST) {
            Post post = postMapper.selectById(targetId);
            if (post != null) {
                incrementPostCounter(post.getId(), "like_count", delta);
                if (delta > 0) {
                    notificationService.sendNotification(post.getUserId(), operatorId,
                            NotificationTypeEnum.LIKE.getCode(), "收到点赞", "你的帖子收到了点赞",
                            TargetTypeEnum.POST.getCode(), post.getId());
                }
            }
        } else if (type == TargetTypeEnum.COMMENT) {
            Comment comment = commentMapper.selectById(targetId);
            if (comment != null) {
                commentMapper.update(null, new LambdaUpdateWrapper<Comment>()
                        .eq(Comment::getId, comment.getId())
                        .setSql("like_count = GREATEST(0, like_count + " + delta + ")"));
                if (delta > 0) {
                    notificationService.sendNotification(comment.getUserId(), operatorId,
                            NotificationTypeEnum.LIKE.getCode(), "收到点赞", "你的评论收到了点赞",
                            TargetTypeEnum.COMMENT.getCode(), comment.getId());
                }
            }
        }
    }

    private Map<Long, String> loadNicknames(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getNickname, (a, b) -> a));
    }

    private void incrementPostCounter(Long postId, String column, int delta) {
        postMapper.update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, postId)
                .setSql(column + " = GREATEST(0, " + column + " + " + delta + ")"));
    }

    private Long currentUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return userId;
    }
}
