package com.xiaoyang.d_game.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaoyang.d_game.common.BizException;
import com.xiaoyang.d_game.common.PageResult;
import com.xiaoyang.d_game.common.ResultCode;
import com.xiaoyang.d_game.common.enums.CommentSortTypeEnum;
import com.xiaoyang.d_game.common.enums.ContentStatusEnum;
import com.xiaoyang.d_game.common.enums.NotificationTypeEnum;
import com.xiaoyang.d_game.common.enums.TargetTypeEnum;
import com.xiaoyang.d_game.dto.CommentCreateReq;
import com.xiaoyang.d_game.dto.CommentResp;
import com.xiaoyang.d_game.dto.InteractionStatusResp;
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
import com.xiaoyang.d_game.security.CurrentUser;
import com.xiaoyang.d_game.service.InteractService;
import com.xiaoyang.d_game.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用户互动业务实现。
 *
 * <p>评论、点赞、收藏和关注都会写关系表或计数字段。涉及多步写入的方法使用事务，
 * 计数字段使用数据库原子表达式更新，减少并发请求下的覆盖问题。</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InteractServiceImpl implements InteractService {

    private static final String LIKE_CACHE_KEY_PREFIX = "interaction:like:";
    private static final String FAVORITE_CACHE_KEY_PREFIX = "interaction:favorite:";

    private final CommentMapper commentMapper;
    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final UserLikeMapper userLikeMapper;
    private final UserFavoriteMapper userFavoriteMapper;
    private final UserFollowMapper userFollowMapper;
    private final NotificationService notificationService;
    private final StringRedisTemplate stringRedisTemplate;

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
    public PageResult<CommentResp> pageComments(Long postId, Long page, Long size, CommentSortTypeEnum sort) {
        Page<Comment> result = new Page<>(page, size);
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<Comment>()
                .eq(Comment::getPostId, postId)
                .eq(Comment::getStatus, ContentStatusEnum.APPROVED.getCode());
        switch (sort) {
            case DEFAULT -> wrapper.orderByDesc(Comment::getLikeCount).orderByAsc(Comment::getGmtCreate);
            case EARLIEST -> wrapper.orderByAsc(Comment::getGmtCreate);
            case LATEST -> wrapper.orderByDesc(Comment::getGmtCreate);
        }
        commentMapper.selectPage(result, wrapper);
        List<Comment> records = result.getRecords();
        Map<Long, User> userMap = loadCommentUsers(records.stream().map(Comment::getUserId).distinct().toList());
        List<CommentResp> respList = records.stream().map(c -> {
            CommentResp resp = new CommentResp();
            User commentAuthor = userMap.get(c.getUserId());
            resp.setId(c.getId());
            resp.setPostId(c.getPostId());
            resp.setParentId(c.getParentId());
            resp.setUserId(c.getUserId());
            resp.setUserNickname(commentAuthor == null ? "已注销用户"
                    : StringUtils.hasText(commentAuthor.getNickname()) ? commentAuthor.getNickname() : commentAuthor.getUsername());
            resp.setUserAvatarUrl(commentAuthor == null ? null : commentAuthor.getAvatarUrl());
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
    /**
     * 查询当前用户对目标的互动状态。
     *
     * <p>Redis Set 只保存正向缓存：如果集合里有当前用户 ID，说明用户已互动；
     * 如果集合里没有，则回源数据库确认，避免 Redis 缓存丢失导致误判。</p>
     */
    public InteractionStatusResp getStatus(Integer targetType, Long targetId) {
        Long userId = currentUserId();
        if (targetType == null || targetId == null) {
            throw new BizException(ResultCode.BAD_REQUEST);
        }
        TargetTypeEnum.of(targetType);

        InteractionStatusResp resp = new InteractionStatusResp();
        resp.setLiked(isInteractionCachedOrPersisted(true, targetType, targetId, userId));
        resp.setFavorited(isInteractionCachedOrPersisted(false, targetType, targetId, userId));
        return resp;
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
        // 清理历史逻辑删除记录，释放 user_id + target_type + target_id 唯一键。
        userLikeMapper.deletePhysicallyByUserAndTarget(userId, targetType, targetId);
        // 关系表先记录“谁点赞了什么”，再维护目标上的冗余计数。
        UserLike like = new UserLike();
        like.setUserId(userId);
        like.setTargetType(targetType);
        like.setTargetId(targetId);
        userLikeMapper.insert(like);
        adjustLikeCount(type, targetId, 1, userId);
        cacheInteractionAfterCommit(true, targetType, targetId, userId, true);
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
        userLikeMapper.deletePhysicallyByUserAndTarget(userId, targetType, targetId);
        adjustLikeCount(type, targetId, -1, userId);
        cacheInteractionAfterCommit(true, targetType, targetId, userId, false);
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
        // 清理历史逻辑删除记录，释放 user_id + target_type + target_id 唯一键。
        userFavoriteMapper.deletePhysicallyByUserAndTarget(userId, targetType, targetId);
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
        cacheInteractionAfterCommit(false, targetType, targetId, userId, true);
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
        userFavoriteMapper.deletePhysicallyByUserAndTarget(userId, targetType, targetId);
        if (Objects.equals(targetType, TargetTypeEnum.POST.getCode())) {
            Post post = postMapper.selectById(targetId);
            if (post != null && post.getFavoriteCount() > 0) {
                incrementPostCounter(post.getId(), "favorite_count", -1);
            }
        }
        cacheInteractionAfterCommit(false, targetType, targetId, userId, false);
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
        lockFollower(userId);
        int restored = userFollowMapper.restoreFollow(userId, followeeId);
        if (restored == 0) {
            UserFollow existing = userFollowMapper.selectOne(new LambdaQueryWrapper<UserFollow>()
                    .eq(UserFollow::getFollowerId, userId)
                    .eq(UserFollow::getFolloweeId, followeeId)
                    .last("LIMIT 1"));
            if (existing != null) {
                return;
            }
            UserFollow relation = new UserFollow();
            relation.setFollowerId(userId);
            relation.setFolloweeId(followeeId);
            try {
                userFollowMapper.insert(relation);
            } catch (org.springframework.dao.DuplicateKeyException exception) {
                if (userFollowMapper.findActiveFollowForUpdate(userId, followeeId) != null) {
                    return;
                }
                throw exception;
            }
        }
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
        lockFollower(userId);
        UserFollow existing = userFollowMapper.selectOne(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, userId)
                .eq(UserFollow::getFolloweeId, followeeId)
                .last("LIMIT 1"));
        if (existing == null) {
            return;
        }
        userFollowMapper.deleteById(existing.getId());
    }

    /** 按关注者串行化关注和取消关注，避免不存在关系时并发插入产生间隙锁冲突。 */
    private void lockFollower(Long userId) {
        userMapper.selectOne(new LambdaQueryWrapper<User>()
                .select(User::getId).eq(User::getId, userId).last("FOR UPDATE"));
    }

    /**
     * 优先使用 Redis Set 判断状态；Redis 未命中时回源数据库。
     */
    private boolean isInteractionCachedOrPersisted(boolean like, Integer targetType, Long targetId, Long userId) {
        String key = interactionCacheKey(like, targetType, targetId);
        String member = String.valueOf(userId);
        try {
            if (Boolean.TRUE.equals(stringRedisTemplate.opsForSet().isMember(key, member))) {
                return true;
            }
        } catch (RuntimeException e) {
            log.warn("查询互动状态 Redis 缓存失败, key={}, userId={}", key, userId, e);
        }

        boolean exists = like ? existsLike(targetType, targetId, userId) : existsFavorite(targetType, targetId, userId);
        if (exists) {
            cacheInteraction(like, targetType, targetId, userId, true);
        }
        return exists;
    }

    private boolean existsLike(Integer targetType, Long targetId, Long userId) {
        return userLikeMapper.selectCount(new LambdaQueryWrapper<UserLike>()
                .eq(UserLike::getUserId, userId)
                .eq(UserLike::getTargetType, targetType)
                .eq(UserLike::getTargetId, targetId)) > 0;
    }

    private boolean existsFavorite(Integer targetType, Long targetId, Long userId) {
        return userFavoriteMapper.selectCount(new LambdaQueryWrapper<UserFavorite>()
                .eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getTargetType, targetType)
                .eq(UserFavorite::getTargetId, targetId)) > 0;
    }

    /**
     * 写操作提交成功后再同步 Redis，避免业务事务回滚但缓存已更新。
     */
    private void cacheInteractionAfterCommit(boolean like, Integer targetType, Long targetId, Long userId, boolean add) {
        Runnable task = () -> cacheInteraction(like, targetType, targetId, userId, add);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
            return;
        }
        task.run();
    }

    /**
     * 同步 Redis Set。缓存失败只影响加速查询，不影响数据库事实。
     */
    private void cacheInteraction(boolean like, Integer targetType, Long targetId, Long userId, boolean add) {
        String key = interactionCacheKey(like, targetType, targetId);
        String member = String.valueOf(userId);
        try {
            if (add) {
                stringRedisTemplate.opsForSet().add(key, member);
            } else {
                stringRedisTemplate.opsForSet().remove(key, member);
            }
        } catch (RuntimeException e) {
            log.warn("同步互动状态 Redis 缓存失败, key={}, userId={}, add={}", key, userId, add, e);
        }
    }

    private String interactionCacheKey(boolean like, Integer targetType, Long targetId) {
        String prefix = like ? LIKE_CACHE_KEY_PREFIX : FAVORITE_CACHE_KEY_PREFIX;
        return prefix + targetType + ":" + targetId;
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
    private Map<Long, User> loadCommentUsers(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
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
        Long userId = CurrentUser.getUserId();
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return userId;
    }
}
