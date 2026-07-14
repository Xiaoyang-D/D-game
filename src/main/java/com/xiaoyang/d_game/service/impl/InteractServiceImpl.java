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
/**
 * 用户互动业务实现。
 *
 * <p>评论、点赞、收藏和关注都会写关系表或计数字段。涉及多步写入的方法使用事务，
 * 计数字段使用数据库原子表达式更新，减少并发请求下的覆盖问题。</p>
 */
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
    /**
     * 创建评论。
     *
     * <p>评论默认直接通过；如果后续接入评论审核，可以把状态改为待审核并调整通知触发时机。</p>
     */
    public Long createComment(CommentCreateReq req) {
        Long userId = currentUserId();
        Post post = postMapper.selectById(req.getPostId());
        if (post == null) {
            throw new BizException(ResultCode.POST_NOT_FOUND);
        }
        if (req.getParentId() != null && req.getParentId() > 0) {
            // 父评论存在性校验，避免创建悬空楼中楼。
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

        // 评论入库后同步增加帖子评论数。
        incrementPostCounter(post.getId(), "comment_count", 1);

        // 通知帖子作者有人评论；发送给自己时 NotificationService 会自动忽略。
        notificationService.sendNotification(post.getUserId(), userId, NotificationTypeEnum.COMMENT.getCode(),
                "收到新评论", "你的帖子收到了新评论", TargetTypeEnum.POST.getCode(), post.getId());
        return comment.getId();
    }

    @Override
    /**
     * 分页查询评论。
     */
    public PageResult<CommentResp> pageComments(Long postId, Long page, Long size) {
        Page<Comment> result = new Page<>(page, size);
        commentMapper.selectPage(result, new LambdaQueryWrapper<Comment>()
                .eq(Comment::getPostId, postId)
                .eq(Comment::getStatus, ContentStatusEnum.APPROVED.getCode())
                .orderByAsc(Comment::getGmtCreate));
        List<Comment> records = result.getRecords();
        // 批量加载昵称，避免每条评论单独查用户表。
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
    /**
     * 点赞目标。
     */
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
        // 关系表先记录“谁点赞了什么”，再维护目标上的冗余计数。
        UserLike like = new UserLike();
        like.setUserId(userId);
        like.setTargetType(targetType);
        like.setTargetId(targetId);
        userLikeMapper.insert(like);
        adjustLikeCount(type, targetId, 1, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    /**
     * 取消点赞。
     */
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
    /**
     * 收藏目标。
     */
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
            // 只有帖子维护 favorite_count，游戏收藏当前只保存关系。
            Post post = postMapper.selectById(targetId);
            if (post != null) {
                incrementPostCounter(post.getId(), "favorite_count", 1);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    /**
     * 取消收藏目标。
     */
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
    /**
     * 关注用户。
     */
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
    /**
     * 取消关注用户。
     */
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

    /**
     * 根据目标类型调整点赞数并发送通知。
     *
     * <p>帖子和评论的计数字段位于不同表，因此这里按目标类型分派。</p>
     */
    private void adjustLikeCount(TargetTypeEnum type, Long targetId, int delta, Long operatorId) {
        if (type == TargetTypeEnum.POST) {
            Post post = postMapper.selectById(targetId);
            if (post != null) {
                incrementPostCounter(post.getId(), "like_count", delta);
                if (delta > 0) {
                    // 只在点赞时发通知，取消点赞不打扰作者。
                    notificationService.sendNotification(post.getUserId(), operatorId,
                            NotificationTypeEnum.LIKE.getCode(), "收到点赞", "你的帖子收到了点赞",
                            TargetTypeEnum.POST.getCode(), post.getId());
                }
            }
        } else if (type == TargetTypeEnum.COMMENT) {
            Comment comment = commentMapper.selectById(targetId);
            if (comment != null) {
                // GREATEST 防止并发取消点赞或历史脏数据导致计数变成负数。
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

    /**
     * 批量加载用户昵称。
     */
    private Map<Long, String> loadNicknames(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getNickname, (a, b) -> a));
    }

    /**
     * 原子调整帖子计数字段。
     *
     * <p>column 只由内部固定字符串传入，不接收用户输入，避免 SQL 注入风险。</p>
     */
    private void incrementPostCounter(Long postId, String column, int delta) {
        postMapper.update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, postId)
                .setSql(column + " = GREATEST(0, " + column + " + " + delta + ")"));
    }

    /**
     * 获取当前登录用户 ID，未登录时统一抛业务异常。
     */
    private Long currentUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return userId;
    }
}
