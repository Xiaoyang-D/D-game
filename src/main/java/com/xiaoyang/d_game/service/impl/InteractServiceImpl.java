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
import com.xiaoyang.d_game.messaging.RealtimeGateway;
import com.xiaoyang.d_game.security.CurrentUser;
import com.xiaoyang.d_game.service.InteractService;
import com.xiaoyang.d_game.service.MentionService;
import com.xiaoyang.d_game.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
    private static final int COMMENT_DELETE_BATCH_SIZE = 500;

    private final CommentMapper commentMapper;
    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final UserLikeMapper userLikeMapper;
    private final UserFavoriteMapper userFavoriteMapper;
    private final UserFollowMapper userFollowMapper;
    private final NotificationService notificationService;
    private final MentionService mentionService;
    private final RealtimeGateway realtime;
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
        Post post = lockCommentPost(req.getPostId());
        Comment parent = null;
        if (req.getParentId() != null && req.getParentId() > 0) {
            // 父评论存在性校验，避免创建悬空楼中楼。
            parent = lockComment(req.getParentId(), post.getId());
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
        mentionService.syncComment(comment.getId(), userId, comment.getContent(), req.getMentions());

        // 评论入库后同步增加帖子评论数。
        incrementPostCounter(post.getId(), "comment_count", 1);

        // 通知帖子作者有人评论；发送给自己时 NotificationService 会自动忽略。
        Set<Long> receivers = new LinkedHashSet<>();
        receivers.add(post.getUserId());
        if (parent != null) { receivers.add(parent.getUserId()); }
        for (Long receiver : receivers) {
            notificationService.sendNotification(receiver, userId, NotificationTypeEnum.COMMENT.getCode(),
                    "收到新回复", "有人回复了你的内容", TargetTypeEnum.COMMENT.getCode(), comment.getId());
        }
        return comment.getId();
    }

    /**
     * 删除顶级评论时删除整个楼层；删除楼内回复时仅删除该回复并保留后续回复。
     * 创建和删除统一先锁帖子，保证并发回复不会引用已经删除的评论。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long commentId) {
        Long userId = currentUserId();
        if (commentId == null || commentId <= 0) {
            throw new BizException(ResultCode.BAD_REQUEST);
        }
        // 此次读取只定位所属帖子；取得帖子锁后必须用当前读重新检查评论。
        Comment located = commentMapper.selectById(commentId);
        if (located == null) {
            throw new BizException(ResultCode.COMMENT_NOT_FOUND);
        }
        Post post = lockCommentPost(located.getPostId());
        Comment comment = lockComment(commentId, post.getId());
        if (comment == null) {
            throw new BizException(ResultCode.COMMENT_NOT_FOUND);
        }
        if (!Objects.equals(userId, comment.getUserId())
                && !Objects.equals(userId, post.getUserId()) && !CurrentUser.hasRole("ADMIN")) {
            throw new BizException(ResultCode.FORBIDDEN, "无权删除该评论");
        }

        if (comment.getParentId() == null || comment.getParentId() == 0L) {
            int deletedCount = deleteCommentThread(comment);
            incrementPostCounter(post.getId(), "comment_count", -deletedCount);
            RealtimeGateway.afterCommit(() -> log.info("评论楼层删除成功，operatorId={}，postId={}，commentId={}，deletedCount={}",
                    userId, post.getId(), commentId, deletedCount));
            return;
        }

        Long parentId = comment.getParentId();
        if (parentId == null || parentId <= 0 || Objects.equals(parentId, commentId)
                || lockComment(parentId, post.getId()) == null) {
            parentId = 0L;
        }
        if (commentMapper.deleteById(commentId) != 1) {
            throw new BizException(ResultCode.COMMENT_NOT_FOUND);
        }
        commentMapper.update(null, new LambdaUpdateWrapper<Comment>()
                .eq(Comment::getPostId, post.getId())
                .eq(Comment::getParentId, commentId)
                .set(Comment::getParentId, parentId)
                .set(Comment::getGmtModified, LocalDateTime.now()));
        incrementPostCounter(post.getId(), "comment_count", -1);
        RealtimeGateway.afterCommit(() -> log.info("单条回复删除成功，后续回复已保留，operatorId={}，postId={}，commentId={}",
                userId, post.getId(), commentId));
    }

    /** 按父子关系遍历整个楼层，分页锁定读取保证覆盖其他页及多层回复。 */
    private int deleteCommentThread(Comment root) {
        Set<Long> commentIds = new LinkedHashSet<>();
        commentIds.add(root.getId());
        List<Long> parents = List.of(root.getId());
        while (!parents.isEmpty()) {
            List<Long> nextParents = new ArrayList<>();
            for (int offset = 0; offset < parents.size(); offset += COMMENT_DELETE_BATCH_SIZE) {
                List<Long> parentBatch = parents.subList(offset, Math.min(offset + COMMENT_DELETE_BATCH_SIZE, parents.size()));
                Long lastId = 0L;
                while (true) {
                    List<Comment> children = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                            .select(Comment::getId)
                            .eq(Comment::getPostId, root.getPostId())
                            .in(Comment::getParentId, parentBatch)
                            .gt(Comment::getId, lastId)
                            .orderByAsc(Comment::getId)
                            .last("LIMIT " + COMMENT_DELETE_BATCH_SIZE + " FOR UPDATE"));
                    for (Comment child : children) {
                        if (commentIds.add(child.getId())) {
                            nextParents.add(child.getId());
                        }
                    }
                    if (children.size() < COMMENT_DELETE_BATCH_SIZE) {
                        break;
                    }
                    lastId = children.getLast().getId();
                }
            }
            parents = nextParents;
        }

        List<Long> ids = new ArrayList<>(commentIds);
        int deletedCount = 0;
        for (int offset = 0; offset < ids.size(); offset += COMMENT_DELETE_BATCH_SIZE) {
            List<Long> batch = ids.subList(offset, Math.min(offset + COMMENT_DELETE_BATCH_SIZE, ids.size()));
            int affected = commentMapper.deleteByIds(batch);
            if (affected != batch.size()) {
                throw new BizException(ResultCode.CONFLICT, "评论已变更，请刷新后重试");
            }
            deletedCount += affected;
        }
        return deletedCount;
    }

    /** 在事务内按帖子串行化评论写入，查询明确字段并过滤逻辑删除记录。 */
    private Post lockCommentPost(Long postId) {
        Post post = postMapper.selectOne(new LambdaQueryWrapper<Post>()
                .select(Post::getId, Post::getUserId)
                .eq(Post::getId, postId)
                .last("FOR UPDATE"));
        if (post == null) {
            throw new BizException(ResultCode.POST_NOT_FOUND);
        }
        return post;
    }

    /** 锁定读取避免 MySQL 可重复读快照或 MyBatis 一级缓存返回旧父节点。 */
    private Comment lockComment(Long commentId, Long postId) {
        return commentMapper.selectOne(new LambdaQueryWrapper<Comment>()
                .select(Comment::getId, Comment::getPostId, Comment::getUserId, Comment::getParentId)
                .eq(Comment::getId, commentId)
                .eq(Comment::getPostId, postId)
                .last("FOR UPDATE"));
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
        wrapper.orderByAsc(Comment::getId);
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
            resp.setMentions(mentionService.commentMentions(c.getId()));
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
        if (Objects.equals(followee.getStatus(), 0)) { throw new BizException(ResultCode.USER_BANNED); }
        lockFollowPair(userId, followeeId);
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
            } catch (DuplicateKeyException exception) {
                if (userFollowMapper.findActiveFollowForUpdate(userId, followeeId) != null) {
                    return;
                }
                throw exception;
            }
        }
        notificationService.sendNotification(followeeId, userId, NotificationTypeEnum.FOLLOW.getCode(),
                "新增关注", "有人关注了你", null, null);
        RealtimeGateway.afterCommit(() -> log.info("关注用户成功，userId={}，followeeId={}", userId, followeeId));
        if (userFollowMapper.selectCount(new LambdaQueryWrapper<UserFollow>().eq(UserFollow::getFollowerId, followeeId)
                .eq(UserFollow::getFolloweeId, userId)) > 0) {
            notificationService.sendNotification(userId, followeeId, NotificationTypeEnum.FRIEND.getCode(), "新增好友", "你们已互相关注，可以发送私信", null, null);
            notificationService.sendNotification(followeeId, userId, NotificationTypeEnum.FRIEND.getCode(), "新增好友", "你们已互相关注，可以发送私信", null, null);
            RealtimeGateway.afterCommit(() -> log.info("互相关注已成为好友，userId={}，friendId={}", userId, followeeId));
        }
        realtime.changed(userId, "FRIEND", null, followeeId);
        realtime.changed(followeeId, "FRIEND", null, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    /**
     * 取消关注用户。
     */
    public void unfollow(Long followeeId) {
        Long userId = currentUserId();
        lockFollowPair(userId, followeeId);
        UserFollow existing = userFollowMapper.selectOne(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, userId)
                .eq(UserFollow::getFolloweeId, followeeId)
                .last("LIMIT 1"));
        if (existing == null) {
            return;
        }
        userFollowMapper.deleteById(existing.getId());
        RealtimeGateway.afterCommit(() -> log.info("取消关注成功，相关好友状态已更新，userId={}，followeeId={}", userId, followeeId));
        realtime.changed(userId, "FRIEND", null, followeeId);
        realtime.changed(followeeId, "FRIEND", null, userId);
    }

    /** 与私信发送统一按双方 ID 升序锁定用户，串行化关注变更并避免锁顺序反转。 */
    private void lockFollowPair(Long userId, Long otherId) {
        if (otherId == null || otherId <= 0 || userId.equals(otherId)) { throw new BizException(ResultCode.BAD_REQUEST); }
        for (Long id : List.of(Math.min(userId, otherId), Math.max(userId, otherId))) {
            userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .select(User::getId).eq(User::getId, id).last("FOR UPDATE"));
        }
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
