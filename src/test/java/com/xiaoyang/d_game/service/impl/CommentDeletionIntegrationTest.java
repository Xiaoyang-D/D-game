package com.xiaoyang.d_game.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.xiaoyang.d_game.common.BizException;
import com.xiaoyang.d_game.common.ResultCode;
import com.xiaoyang.d_game.config.MybatisPlusConfig;
import com.xiaoyang.d_game.dto.CommentCreateReq;
import com.xiaoyang.d_game.mapper.CommentMapper;
import com.xiaoyang.d_game.mapper.PostMapper;
import com.xiaoyang.d_game.mapper.UserFavoriteMapper;
import com.xiaoyang.d_game.mapper.UserFollowMapper;
import com.xiaoyang.d_game.mapper.UserLikeMapper;
import com.xiaoyang.d_game.mapper.UserMapper;
import com.xiaoyang.d_game.security.CurrentUserPrincipal;
import com.xiaoyang.d_game.service.InteractService;
import com.xiaoyang.d_game.service.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.URLClassLoader;
import java.nio.file.Path;
import java.sql.Driver;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/** 使用调用者提供的 H2 驱动执行隔离的内存数据库测试，无生产连接或新增项目依赖。 */
@EnabledIfEnvironmentVariable(named = "COMMENT_TEST_DRIVER_JAR", matches = ".+")
class CommentDeletionIntegrationTest {
    private URLClassLoader driverLoader;
    private JdbcTemplate jdbc;
    private InteractService service;
    private CommentMapper comments;
    private DataSourceTransactionManager transactions;

    @BeforeEach
    void initializeIsolatedDatabase() throws Exception {
        driverLoader = new URLClassLoader(new java.net.URL[]{
                Path.of(System.getenv("COMMENT_TEST_DRIVER_JAR")).toUri().toURL()
        }, getClass().getClassLoader());
        Driver driver = (Driver) Class.forName("org.h2.Driver", true, driverLoader).getConstructor().newInstance();
        var source = new SimpleDriverDataSource(driver,
                "jdbc:h2:mem:comment_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000", "sa", "");
        jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE post (id BIGINT PRIMARY KEY, user_id BIGINT, comment_count INT CHECK (comment_count >= 0), "
                + "is_deleted INT DEFAULT 0, gmt_modified TIMESTAMP)");
        jdbc.execute("CREATE TABLE comment (id BIGINT PRIMARY KEY, post_id BIGINT, user_id BIGINT, parent_id BIGINT, "
                + "content VARCHAR(2000), like_count INT DEFAULT 0, status INT DEFAULT 2, "
                + "is_deleted INT DEFAULT 0, gmt_create TIMESTAMP, gmt_modified TIMESTAMP)");
        jdbc.update("INSERT INTO post (id, user_id, comment_count) VALUES (1, 99, 4), (2, 98, 1)");
        jdbc.update("INSERT INTO comment (id, post_id, user_id, parent_id, content) VALUES "
                + "(101, 1, 10, 0, 'root'), (102, 1, 20, 101, 'child'), "
                + "(103, 1, 30, 102, 'grandchild'), (104, 1, 40, 101, 'sibling'), (201, 2, 50, 0, 'other post')");

        var configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.addMapper(CommentMapper.class);
        configuration.addMapper(PostMapper.class);
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(source);
        factory.setConfiguration(configuration);
        factory.setGlobalConfig(new GlobalConfig().setDbConfig(new GlobalConfig.DbConfig())
                .setMetaObjectHandler(new MybatisPlusConfig().metaObjectHandler()));
        factory.setPlugins(new MybatisPlusConfig().mybatisPlusInterceptor());
        var session = new SqlSessionTemplate(factory.getObject());
        comments = session.getMapper(CommentMapper.class);
        var target = new InteractServiceImpl(comments, session.getMapper(PostMapper.class), mock(UserMapper.class),
                mock(UserLikeMapper.class), mock(UserFavoriteMapper.class), mock(UserFollowMapper.class),
                mock(NotificationService.class), mock(StringRedisTemplate.class));
        transactions = new DataSourceTransactionManager(source);
        var interceptor = new TransactionInterceptor();
        interceptor.setTransactionManager(transactions);
        interceptor.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        var proxy = new ProxyFactory(target);
        proxy.addAdvice(interceptor);
        service = (InteractService) proxy.getProxy();
        authenticate(10L, false);
    }

    @AfterEach
    void cleanup() throws Exception {
        SecurityContextHolder.clearContext();
        if (jdbc != null) {
            jdbc.execute("SHUTDOWN");
        }
        if (driverLoader != null) {
            driverLoader.close();
        }
    }

    @ParameterizedTest
    @ValueSource(longs = {10L, 99L, 77L})
    void authorPostOwnerAndAdminCanDeleteTheEntireFloor(long userId) {
        authenticate(userId, userId == 77L);
        service.deleteComment(101L);
        assertNull(comments.selectById(101L));
        assertEquals(1, number("SELECT is_deleted FROM comment WHERE id=101"));
        assertNull(comments.selectById(102L));
        assertNull(comments.selectById(103L));
        assertNull(comments.selectById(104L));
        assertEquals(0, count());
        assertEquals(1, number("SELECT COUNT(*) FROM comment WHERE is_deleted=0"));
        assertEquals(1, number("SELECT comment_count FROM post WHERE id=2"));
    }

    @Test
    void unauthorizedRequestsDoNotWriteAnything() {
        authenticate(77L, false);
        assertCode(ResultCode.FORBIDDEN, () -> service.deleteComment(101L));
        SecurityContextHolder.clearContext();
        assertCode(ResultCode.UNAUTHORIZED, () -> service.deleteComment(101L));
        assertEquals(0, number("SELECT is_deleted FROM comment WHERE id=101"));
        assertEquals(101, parent(102L));
        assertEquals(4, count());
    }

    @Test
    void invalidMissingAndAlreadyDeletedCommentsNeverDecrementAgain() {
        assertCode(ResultCode.BAD_REQUEST, () -> service.deleteComment(0L));
        assertCode(ResultCode.BAD_REQUEST, () -> service.deleteComment(null));
        assertCode(ResultCode.COMMENT_NOT_FOUND, () -> service.deleteComment(999L));
        service.deleteComment(101L);
        assertCode(ResultCode.COMMENT_NOT_FOUND, () -> service.deleteComment(101L));
        assertEquals(0, count());
    }

    @Test
    void missingPostPreventsDeletion() {
        jdbc.update("UPDATE post SET is_deleted=1 WHERE id=1");
        assertCode(ResultCode.POST_NOT_FOUND, () -> service.deleteComment(101L));
        assertNotNull(comments.selectById(101L));
    }

    @Test
    void deletingNestedCommentMovesOnlyItsDirectReplies() {
        authenticate(20L, false);
        service.deleteComment(102L);
        assertEquals(101, parent(103L));
        assertEquals(101, parent(104L));
        assertEquals(3, count());
        assertNotNull(comments.selectById(101L));
        assertNotNull(comments.selectById(103L));
        assertNotNull(comments.selectById(104L));
    }

    @Test
    void unavailableParentPromotesSurvivingReplies() {
        jdbc.update("UPDATE comment SET is_deleted=1 WHERE id=101");
        authenticate(20L, false);
        service.deleteComment(102L);
        assertEquals(0, parent(103L));
    }

    @Test
    void countersDoNotGoNegative() {
        jdbc.update("UPDATE post SET comment_count=0 WHERE id=1");
        service.deleteComment(101L);
        assertEquals(0, count());
    }

    @Test
    void counterFailureRollsBackDeletionAndReplyMigration() {
        jdbc.execute("ALTER TABLE post ADD CONSTRAINT fail_decrement CHECK (comment_count <> 0)");
        assertThrows(RuntimeException.class, () -> service.deleteComment(101L));
        assertNotNull(comments.selectById(101L));
        assertEquals(101, parent(102L));
        assertEquals(101, parent(104L));
        assertEquals(4, count());
        assertEquals(4, number("SELECT COUNT(*) FROM comment WHERE post_id=1 AND is_deleted=0"));
    }

    @Test
    void nestedReplyDeletionRollsBackWhenCounterFails() {
        jdbc.execute("ALTER TABLE post ADD CONSTRAINT fail_decrement CHECK (comment_count <> 3)");
        authenticate(20L, false);
        assertThrows(RuntimeException.class, () -> service.deleteComment(102L));
        assertNotNull(comments.selectById(102L));
        assertEquals(102, parent(103L));
        assertEquals(4, count());
    }

    @Test
    void creatingRepliesRequiresAnExistingParentInTheSamePost() {
        assertCode(ResultCode.COMMENT_NOT_FOUND, () -> service.createComment(reply(201L)));
        service.deleteComment(101L);
        assertCode(ResultCode.COMMENT_NOT_FOUND, () -> service.createComment(reply(101L)));
        Long rootId = service.createComment(reply(0L));
        Long id = service.createComment(reply(rootId));
        assertEquals(rootId.longValue(), parent(id));
        assertEquals(2, count());
    }

    @Test
    void concurrentDuplicateDeletesOnlyDecrementOnce() throws Exception {
        concurrently(() -> service.deleteComment(101L), () -> {
            assertCode(ResultCode.COMMENT_NOT_FOUND, () -> service.deleteComment(101L));
        });
        assertEquals(0, count());
        assertNull(comments.selectById(102L));
    }

    @Test
    void concurrentReplyAfterDeletionCannotReferenceTheDeletedParent() throws Exception {
        concurrently(() -> service.deleteComment(101L), () -> {
            assertCode(ResultCode.COMMENT_NOT_FOUND, () -> service.createComment(reply(101L)));
        });
        assertEquals(0, count());
        assertEquals(0, number("SELECT COUNT(*) FROM comment WHERE parent_id=101 AND is_deleted=0"));
    }

    @Test
    void replyCommittedBeforeFloorDeletionIsAlsoDeleted() throws Exception {
        concurrently(() -> service.createComment(reply(101L)), () -> service.deleteComment(101L));
        assertEquals(0, count());
        assertEquals(0, number("SELECT COUNT(*) FROM comment WHERE parent_id=101 AND is_deleted=0"));
        assertEquals(0, number("SELECT COUNT(*) FROM comment WHERE post_id=1 AND is_deleted=0"));
    }

    @Test
    void floorDeletionMakesConcurrentReplyDeletionANoOp() throws Exception {
        concurrently(() -> service.deleteComment(101L), () -> {
            assertCode(ResultCode.COMMENT_NOT_FOUND, () -> service.deleteComment(102L));
        });
        assertEquals(0, count());
    }

    @Test
    void replyDeletionBeforeFloorDeletionKeepsDescendantsInTheFloor() throws Exception {
        concurrently(() -> service.deleteComment(102L), () -> service.deleteComment(101L));
        assertEquals(0, count());
        assertNull(comments.selectById(103L));
    }

    @Test
    void replyToANestedCommentCommittedBeforeDeletionIsPreserved() throws Exception {
        concurrently(() -> service.createComment(reply(102L)), () -> service.deleteComment(102L));
        assertEquals(4, count());
        assertEquals(101, parent(103L));
        assertEquals(3, number("SELECT COUNT(*) FROM comment WHERE parent_id=101 AND is_deleted=0"));
    }

    @Test
    void consecutiveReplyDeletesDoNotRemoveSiblingReplies() throws Exception {
        concurrently(() -> service.deleteComment(102L), () -> service.deleteComment(103L));
        assertEquals(2, count());
        assertNotNull(comments.selectById(101L));
        assertNotNull(comments.selectById(104L));
    }

    @Test
    void floorDeletionCoversMultipleBatchesHiddenRepliesAndOtherPages() {
        List<Object[]> rows = new java.util.ArrayList<>();
        for (long id = 1000; id < 1501; id++) {
            rows.add(new Object[]{id, 1L, 50L, 101L, "batch reply", 3});
        }
        jdbc.batchUpdate("INSERT INTO comment (id, post_id, user_id, parent_id, content, status) VALUES (?, ?, ?, ?, ?, ?)", rows);
        jdbc.update("INSERT INTO comment (id, post_id, user_id, parent_id, content) VALUES (105, 1, 10, 0, 'other floor')");
        jdbc.update("UPDATE post SET comment_count=506 WHERE id=1");
        service.deleteComment(101L);
        assertEquals(1, count());
        assertEquals(505, number("SELECT COUNT(*) FROM comment WHERE post_id=1 AND is_deleted=1"));
        assertNotNull(comments.selectById(105L));
        assertNotNull(comments.selectById(201L));
    }

    /** 先让第一事务完成写入并持有帖子锁，再启动第二事务并验证它阻塞至提交。 */
    private void concurrently(Runnable first, Runnable second) throws Exception {
        var threadNumber = new AtomicInteger();
        var executor = new ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(2),
                task -> new Thread(task, "comment-deletion-test-" + threadNumber.incrementAndGet()),
                new ThreadPoolExecutor.AbortPolicy());
        var holdingLock = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var secondStarted = new CountDownLatch(1);
        try {
            var firstResult = executor.submit(() -> {
                authenticate(99L, false);
                try {
                    new TransactionTemplate(transactions).executeWithoutResult(status -> {
                        first.run();
                        holdingLock.countDown();
                        await(release);
                    });
                } finally {
                    SecurityContextHolder.clearContext();
                }
            });
            assertTrue(holdingLock.await(5, TimeUnit.SECONDS));
            var secondResult = executor.submit(() -> {
                authenticate(99L, false);
                try {
                    secondStarted.countDown();
                    second.run();
                } finally {
                    SecurityContextHolder.clearContext();
                }
            });
            assertTrue(secondStarted.await(5, TimeUnit.SECONDS));
            assertThrows(java.util.concurrent.TimeoutException.class, () -> secondResult.get(200, TimeUnit.MILLISECONDS));
            release.countDown();
            firstResult.get(5, TimeUnit.SECONDS);
            secondResult.get(5, TimeUnit.SECONDS);
        } finally {
            release.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for test transaction");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted test transaction", e);
        }
    }

    private static void authenticate(long userId, boolean admin) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new CurrentUserPrincipal(userId, "test"), null,
                List.of(new SimpleGrantedAuthority(admin ? "ROLE_ADMIN" : "ROLE_USER"))));
    }

    private static CommentCreateReq reply(Long parentId) {
        CommentCreateReq req = new CommentCreateReq();
        req.setPostId(1L);
        req.setParentId(parentId);
        req.setContent("retained reply");
        return req;
    }

    private static void assertCode(ResultCode code, Runnable action) {
        assertEquals(code.getCode(), assertThrows(BizException.class, action::run).getCode());
    }

    private long parent(Long id) {
        return jdbc.queryForObject("SELECT parent_id FROM comment WHERE id=?", Long.class, id);
    }

    private int count() {
        return number("SELECT comment_count FROM post WHERE id=1");
    }

    private int number(String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }
}
