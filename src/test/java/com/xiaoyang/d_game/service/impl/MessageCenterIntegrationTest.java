package com.xiaoyang.d_game.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoyang.d_game.common.*;
import com.xiaoyang.d_game.config.MybatisPlusConfig;
import com.xiaoyang.d_game.dto.*;
import com.xiaoyang.d_game.entity.*;
import com.xiaoyang.d_game.mapper.*;
import com.xiaoyang.d_game.messaging.RealtimeGateway;
import com.xiaoyang.d_game.messaging.notification.*;
import com.xiaoyang.d_game.security.CurrentUserPrincipal;
import com.xiaoyang.d_game.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Driver;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 真正的 MP SQL 与事务，数据库仅为随机命名内存库，驱动通过外部环境显式提供。 */
@EnabledIfEnvironmentVariable(named="COMMENT_TEST_DRIVER_JAR", matches=".+")
class MessageCenterIntegrationTest {
    private URLClassLoader driverLoader;
    private JdbcTemplate jdbc;
    private SqlSessionTemplate session;
    private DataSourceTransactionManager transactions;
    private ChatService chat;
    private NotificationService notifications;
    private MentionService mentions;
    private InteractService interactions;
    private RealtimeGateway realtime;
    private ObjectMapper json;

    @BeforeEach void initialize() throws Exception {
        driverLoader = new URLClassLoader(new URL[]{Path.of(System.getenv("COMMENT_TEST_DRIVER_JAR")).toUri().toURL()}, getClass().getClassLoader());
        Driver driver = (Driver) Class.forName("org.h2.Driver", true, driverLoader).getConstructor().newInstance();
        var source = new SimpleDriverDataSource(driver, "jdbc:h2:mem:messages_" + UUID.randomUUID()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000", "sa", "");
        jdbc = new JdbcTemplate(source);
        String schema;
        try (var input = new ClassPathResource("db/schema.sql").getInputStream()) { schema = new String(input.readAllBytes(), StandardCharsets.UTF_8); }
        for (String table : List.of("user", "user_follow", "post", "comment", "notification", "chat_conversation", "chat_member", "chat_message", "content_mention", "notification_setting", "notification_outbox")) {
            var matcher = Pattern.compile("CREATE TABLE IF NOT EXISTS `?" + table + "`? \\(.*?\\) ENGINE=.*?;", Pattern.DOTALL).matcher(schema);
            assertTrue(matcher.find(), table);
            // MySQL 的索引名按表隔离，H2 按 schema 隔离；仅在测试中加表名前缀。
            jdbc.execute(matcher.group().replaceAll("(KEY )`([^`]+)`", "$1`" + table + "_$2`"));
        }
        jdbc.update("INSERT INTO `user`(id,username,password_hash,nickname) VALUES (10,'u10','test-hash','Alpha'),(20,'u20','test-hash','Beta'),(30,'u30','test-hash','Gamma'),(99,'u99','test-hash','Owner')");
        jdbc.update("INSERT INTO user_follow(id,follower_id,followee_id) VALUES (1,10,20),(2,20,10)");
        jdbc.update("INSERT INTO post(id,board_id,user_id,title,content,status) VALUES(100,1,99,'title','body',2)");
        jdbc.update("INSERT INTO comment(id,post_id,user_id,parent_id,content,status) VALUES(200,100,20,0,'parent',2)");
        var configuration = new MybatisConfiguration(); configuration.setMapUnderscoreToCamelCase(true);
        var global = new GlobalConfig().setDbConfig(new GlobalConfig.DbConfig()).setMetaObjectHandler(new MybatisPlusConfig().metaObjectHandler());
        GlobalConfigUtils.setGlobalConfig(configuration, global);
        for (Class<?> mapper : List.of(ChatConversationMapper.class, ChatMemberMapper.class, ChatMessageMapper.class,
                UserMapper.class, UserFollowMapper.class, CommentMapper.class, PostMapper.class, ContentMentionMapper.class,
                NotificationSettingMapper.class, NotificationOutboxMapper.class, NotificationMapper.class)) { configuration.addMapper(mapper); }
        var factory = new MybatisSqlSessionFactoryBean(); factory.setDataSource(source); factory.setConfiguration(configuration);
        factory.setGlobalConfig(global);
        factory.setPlugins(new MybatisPlusConfig().mybatisPlusInterceptor());
        factory.setMapperLocations(new ClassPathResource("mapper/ChatConversationMapper.xml"), new ClassPathResource("mapper/CommentMapper.xml"));
        session = new SqlSessionTemplate(factory.getObject()); transactions = new DataSourceTransactionManager(source);
        realtime = mock(RealtimeGateway.class); json = new ObjectMapper().findAndRegisterModules();
        var notificationTarget = new NotificationServiceImpl(mapper(NotificationSettingMapper.class), mapper(NotificationOutboxMapper.class), json,
                mapper(UserMapper.class), mapper(PostMapper.class), mapper(CommentMapper.class), realtime);
        ReflectionTestUtils.setField(notificationTarget, "baseMapper", mapper(NotificationMapper.class));
        notifications = proxy(notificationTarget);
        mentions = proxy(new MentionServiceImpl(mapper(UserMapper.class), mapper(ContentMentionMapper.class), mapper(CommentMapper.class), mapper(PostMapper.class), notifications, new HtmlSanitizer(), json));
        chat = proxy(new ChatServiceImpl(mapper(ChatConversationMapper.class), mapper(ChatMemberMapper.class), mapper(ChatMessageMapper.class), mapper(UserMapper.class), mapper(UserFollowMapper.class), realtime));
        interactions = proxy(new InteractServiceImpl(mapper(CommentMapper.class), mapper(PostMapper.class), mapper(UserMapper.class),
                mock(UserLikeMapper.class), mock(UserFavoriteMapper.class), mapper(UserFollowMapper.class), notifications, mentions, realtime,
                mock(StringRedisTemplate.class)));
        as(10);
    }
    private <T> T mapper(Class<T> type) { return session.getMapper(type); }
    @SuppressWarnings("unchecked") private <T> T proxy(T target) {
        var interceptor = new TransactionInterceptor(); interceptor.setTransactionManager(transactions);
        interceptor.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        var proxy = new ProxyFactory(target); proxy.setProxyTargetClass(true); proxy.addAdvice(interceptor); return (T) proxy.getProxy();
    }
    private void as(long id) { SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new CurrentUserPrincipal(id, "test"), null, List.of())); }
    private long number(String sql) { return jdbc.queryForObject(sql, Long.class); }
    private MessageDtos.Message send(Long conversation, String content) { return chat.send(conversation, new MessageDtos.Send(content, UUID.randomUUID().toString())); }
    @AfterEach void cleanup() throws Exception { SecurityContextHolder.clearContext(); if (jdbc != null) jdbc.execute("SHUTDOWN"); if (driverLoader != null) driverLoader.close(); }

    @Test void existingMutualFollowersAreFriendsAndConversationIsUnique() {
        assertEquals(20L, chat.friends(1,20,"").getRecords().getFirst().id());
        var first = chat.open(20L); assertEquals(first.id(), chat.open(20L).id()); as(20); assertEquals(first.id(), chat.open(10L).id());
        assertEquals(1, number("SELECT COUNT(id) FROM chat_conversation"));
    }
    @Test void singleFollowingAndStrangersCannotOpenChat() {
        jdbc.update("UPDATE user_follow SET is_deleted=1 WHERE id=2");
        assertThrows(BizException.class, () -> chat.open(20L)); assertThrows(BizException.class, () -> chat.open(30L));
    }
    @Test void selfBannedAndAnonymousRequestsFail() {
        assertThrows(BizException.class, () -> chat.open(10L)); jdbc.update("UPDATE `user` SET status=0 WHERE id=20");
        assertThrows(BizException.class, () -> chat.open(20L)); SecurityContextHolder.clearContext(); assertThrows(BizException.class, () -> chat.friends(1,20,""));
    }
    @Test void sendingPreservesEmojiAndRetryDoesNotDuplicate() {
        Long id = chat.open(20L).id(); var request = new MessageDtos.Send("hello 😀\nworld", UUID.randomUUID().toString());
        var first = chat.send(id, request); var again = chat.send(id, request);
        assertEquals(first.id(), again.id()); assertEquals(1L, first.seq()); assertEquals("hello 😀\nworld", again.content());
        assertEquals(1, number("SELECT COUNT(id) FROM chat_message")); as(20); assertEquals(1, chat.unread());
        assertEquals(1, chat.history(id,null,null,50).records().size());
    }
    @Test void unfollowDisablesSendingButPreservesHistoryAndRecall() {
        Long id = chat.open(20L).id(); var sent = send(id,"one"); interactions.unfollow(20L);
        assertFalse(chat.detail(id).isFriend()); assertThrows(BizException.class, () -> send(id,"two"));
        assertEquals(1, chat.history(id,null,null,50).records().size()); chat.recall(sent.id()); assertTrue(chat.history(id,null,null,50).records().getFirst().recalled());
        interactions.follow(20L); assertTrue(chat.detail(id).isFriend()); send(id,"restored");
    }
    @Test void recallErasesOriginalForBothAndIsIdempotent() {
        Long id = chat.open(20L).id(); var sent = send(id,"secret test text"); chat.recall(sent.id()); chat.recall(sent.id());
        assertEquals("", jdbc.queryForObject("SELECT content FROM chat_message WHERE id=?",String.class,sent.id()));
        as(20); assertEquals("",chat.history(id,null,null,50).records().getFirst().content()); assertEquals(0,chat.unread());
    }
    @Test void recallRejectsOtherUserAndTwoMinuteBoundary() {
        Long id = chat.open(20L).id(); var sent = send(id,"test"); as(20); assertThrows(BizException.class, () -> chat.recall(sent.id()));
        as(10); jdbc.update("UPDATE chat_message SET gmt_create=? WHERE id=?",LocalDateTime.now().minusMinutes(2),sent.id());
        assertThrows(BizException.class, () -> chat.recall(sent.id()));
    }
    @Test void clearingIsSingleSidedAndOnlyNewMessagesReappear() {
        Long id = chat.open(20L).id(); send(id,"old"); chat.clear(id); chat.clear(id);
        assertEquals(0,chat.conversations(1,20).getTotal()); assertTrue(chat.history(id,null,null,50).records().isEmpty());
        as(20); assertEquals("old",chat.history(id,null,null,50).records().getFirst().content()); send(id,"new");
        as(10); assertEquals(1,chat.conversations(1,20).getTotal()); assertEquals(List.of("new"),chat.history(id,null,null,50).records().stream().map(MessageDtos.Message::content).toList());
    }
    @Test void readingIsMonotonicAndPrivateToRecipient() {
        Long id = chat.open(20L).id(); send(id,"one"); var second = send(id,"two"); as(20); chat.read(id,second.seq()); chat.read(id,1L);
        assertEquals(0,chat.unread()); assertEquals(2,number("SELECT read_seq FROM chat_member WHERE user_id=20"));
        assertThrows(BizException.class, () -> chat.read(id,3L));
        String response = assertDoesNotThrow(() -> json.writeValueAsString(chat.history(id,null,null,50)));
        assertFalse(response.contains("readSeq")); assertFalse(response.contains("isRead"));
    }
    @Test void nonParticipantCannotReadClearSendOrMarkRead() {
        Long id = chat.open(20L).id(); as(30);
        assertThrows(BizException.class, () -> chat.detail(id)); assertThrows(BizException.class, () -> chat.history(id,null,null,50));
        assertThrows(BizException.class, () -> chat.clear(id)); assertThrows(BizException.class, () -> chat.read(id,0L)); assertThrows(BizException.class, () -> send(id,"x"));
    }
    @Test void historyCursorsAreOrderedAndBounded() {
        Long id = chat.open(20L).id(); for(int i=0;i<55;i++) send(id,"m"+i);
        var latest = chat.history(id,null,null,50); assertTrue(latest.hasMore()); assertEquals(6L,latest.records().getFirst().seq());
        assertEquals(5,chat.history(id,6L,null,50).records().size()); assertEquals(5,chat.history(id,null,50L,50).records().size());
        assertThrows(BizException.class, () -> chat.history(id,5L,1L,50));
    }
    @Test void failedTransactionRollsBackMessageAndSequence() {
        Long id = chat.open(20L).id(); doThrow(new IllegalStateException("test failure")).when(realtime).changed(any(),eq("MESSAGE"),any(),any());
        assertThrows(IllegalStateException.class, () -> send(id,"fail")); assertEquals(0,number("SELECT COUNT(id) FROM chat_message")); assertEquals(0,number("SELECT last_seq FROM chat_conversation"));
    }
    @Test void commentNotifiesBothPostOwnerAndDirectParentOnly() {
        var request = new CommentCreateReq(); request.setPostId(100L); request.setParentId(200L); request.setContent("reply"); interactions.createComment(request);
        assertEquals(2,number("SELECT COUNT(id) FROM notification_outbox"));
        jdbc.update("UPDATE comment SET user_id=99 WHERE id=200"); interactions.createComment(request);
        assertEquals(3,number("SELECT COUNT(id) FROM notification_outbox"));
    }
    @Test void mutedNotificationPersistsWithoutUnreadAndPreferenceIsFrozen() throws Exception {
        notifications.saveSettings(new MessageDtos.Settings(true,true,false,true)); notifications.sendNotification(10L,20L,1,"like","test",null,null);
        notifications.saveSettings(new MessageDtos.Settings(true,true,true,true));
        var event = json.readValue(jdbc.queryForObject("SELECT payload FROM notification_outbox",String.class),NotificationEvent.class);
        proxy(new NotificationEventHandler(mapper(NotificationMapper.class),realtime)).handle(event);
        assertEquals(1,notifications.pageNotifications(1L,15L,"like").getTotal()); assertEquals(0,notifications.unreadCount());
    }
    @Test void duplicateConsumerAndRepeatedReadCannotChangeCountsTwice() {
        var handler = proxy(new NotificationEventHandler(mapper(NotificationMapper.class),realtime));
        var event = NotificationEvent.of(10L,20L,2,"reply","body",null,null); handler.handle(event); handler.handle(event);
        assertEquals(1,notifications.unreadCount()); Long id = notifications.pageNotifications(1L,10L).getRecords().getFirst().id();
        notifications.markRead(id); notifications.markRead(id); assertEquals(0,notifications.unreadCount()); as(20); assertThrows(BizException.class, () -> notifications.markRead(id));
    }
    @Test void categoryReadDoesNotAffectOtherCategories() {
        var handler=proxy(new NotificationEventHandler(mapper(NotificationMapper.class),realtime));
        handler.handle(NotificationEvent.of(10L,20L,1,"like","body",null,null)); handler.handle(NotificationEvent.of(10L,20L,2,"reply","body",null,null));
        notifications.markAllRead("reply"); assertEquals(1,notifications.unreadCount()); assertEquals(0,notifications.categoryUnread("reply"));
    }
    @Test void deletedTargetRetainsNotificationWithoutLink() {
        proxy(new NotificationEventHandler(mapper(NotificationMapper.class),realtime)).handle(NotificationEvent.of(10L,20L,2,"reply","body",2,200L));
        assertTrue(notifications.pageNotifications(1L,10L).getRecords().getFirst().targetAvailable()); jdbc.update("UPDATE comment SET is_deleted=1 WHERE id=200");
        var notification=notifications.pageNotifications(1L,10L).getRecords().getFirst(); assertFalse(notification.targetAvailable()); assertNull(notification.targetUrl());
    }
    @Test void mentionUsesUserIdAndInvalidRangesRollBack() {
        var request=new CommentCreateReq(); request.setPostId(100L); request.setContent("@Beta hi"); request.setMentions(List.of(new MessageDtos.Mention(20L,0,5)));
        Long id=interactions.createComment(request); assertEquals(20L,mentions.commentMentions(id).getFirst().userId());
        jdbc.update("UPDATE `user` SET nickname='Renamed' WHERE id=20"); assertEquals(20L,mentions.commentMentions(id).getFirst().userId());
        request.setMentions(List.of(new MessageDtos.Mention(20L,0,99))); long before=number("SELECT COUNT(id) FROM comment"); assertThrows(BizException.class, () -> interactions.createComment(request)); assertEquals(before,number("SELECT COUNT(id) FROM comment"));
    }
    @Test void postDraftDoesNotNotifyAndRepeatedPublicationOrEditDoesNotSpam() {
        Post post=mapper(PostMapper.class).selectById(100L); post.setContent(mentions.normalizePost("<p><span data-type='mention' data-id='20'>@wrong</span></p>")); post.setStatus(0);
        mentions.syncPost(post); assertEquals(0,number("SELECT COUNT(id) FROM notification_outbox")); post.setStatus(2); mentions.syncPost(post); mentions.syncPost(post);
        assertEquals(1,number("SELECT COUNT(id) FROM notification_outbox")); post.setContent("plain"); mentions.syncPost(post);
        post.setContent("<span data-type='mention' data-id='20'>@Beta</span>"); mentions.syncPost(post); assertEquals(1,number("SELECT COUNT(id) FROM notification_outbox"));
        post.setContent("<span data-type='mention' data-id='30'>@Gamma</span>"); mentions.syncPost(post); assertEquals(2,number("SELECT COUNT(id) FROM notification_outbox"));
    }
    @Test void scheduledPostOnlyNotifiesWhenActuallyPublished() {
        var boardMapper=mock(BoardMapper.class); var board=new Board(); board.setId(1L); when(boardMapper.selectById(1L)).thenReturn(board);
        var target=new PostServiceImpl(mock(GameSectionService.class),boardMapper,mock(GameMapper.class),mapper(UserMapper.class),mapper(UserFollowMapper.class),new HtmlSanitizer(),mock(PostTopicMapper.class),mock(PostTopicRelMapper.class),mock(PostCollectionMapper.class),mentions);
        ReflectionTestUtils.setField(target,"baseMapper",mapper(PostMapper.class));
        PostService service=proxy(target);
        jdbc.update("UPDATE post SET status=0,content=?,scheduled_publish_at=? WHERE id=100","<span data-type='mention' data-id='20'>@Beta</span>",LocalDateTime.now().plusHours(1));
        service.publishDuePosts(); assertEquals(0,number("SELECT COUNT(id) FROM notification_outbox")); assertEquals(0,number("SELECT status FROM post WHERE id=100"));
        jdbc.update("UPDATE post SET scheduled_publish_at=? WHERE id=100",LocalDateTime.now().minusHours(1)); service.publishDuePosts(); service.publishDuePosts();
        assertEquals(2,number("SELECT status FROM post WHERE id=100")); assertEquals(1,number("SELECT COUNT(id) FROM notification_outbox"));
    }
    @Test void mentionsHandleSameNicknameAndRejectBannedUsers() {
        jdbc.update("UPDATE `user` SET nickname='Beta' WHERE id=30"); assertEquals(2,mentions.candidates("Beta").size());
        jdbc.update("UPDATE `user` SET status=0 WHERE id=20"); assertEquals(1,mentions.candidates("Beta").size());
        assertThrows(BizException.class, () -> mentions.normalizePost("<span data-type='mention' data-id='20'>@Beta</span>"));
    }
    @Test void transactionRollbackDoesNotLogMessageSuccessOrLeakContent() {
        Long id = chat.open(20L).id();
        try (var logs = new LogCapture(ChatServiceImpl.class)) {
            new TransactionTemplate(transactions).executeWithoutResult(status -> {
                send(id, "PRIVATE-MESSAGE-BODY");
                status.setRollbackOnly();
            });
            assertEquals(0, number("SELECT COUNT(id) FROM chat_message"));
            assertFalse(logs.messages().contains("私信发送成功"));
            send(id, "PRIVATE-MESSAGE-BODY");
            assertTrue(logs.messages().contains("私信发送成功"));
            assertFalse(logs.messages().contains("PRIVATE-MESSAGE-BODY"));
        }
    }
    @Test void commentLocationUsesSameSortAsPaging() {
        for(int i=1;i<=25;i++) jdbc.update("INSERT INTO comment(id,post_id,user_id,parent_id,content,status,like_count,gmt_create) VALUES(?,100,10,0,'x',2,?,?)",200+i,30-i,LocalDateTime.of(2026,1,1,0,0));
        assertEquals(2,mentions.location(225L,"DEFAULT",20).page());
    }
    @Test void outboxRetriesBrokerFailureThenWaitsForDatabaseConsumption() {
        notifications.sendNotification(10L,20L,2,"reply","body",null,null); RabbitTemplate rabbit=mock(RabbitTemplate.class);
        doThrow(new IllegalStateException("broker down")).when(rabbit).convertAndSend(anyString(),anyString(),any(NotificationEvent.class),any(MessagePostProcessor.class),any(CorrelationData.class));
        var dispatcher=new NotificationOutboxDispatcher(mapper(NotificationOutboxMapper.class),mapper(NotificationMapper.class),rabbit,json);
        dispatcher.dispatch(); assertEquals(1,number("SELECT attempts FROM notification_outbox")); assertEquals(0,number("SELECT delivered FROM notification_outbox"));
        reset(rabbit); doAnswer(invocation -> { CorrelationData correlation=invocation.getArgument(4); correlation.getFuture().complete(new CorrelationData.Confirm(true,null)); return null; })
                .when(rabbit).convertAndSend(anyString(),anyString(),any(NotificationEvent.class),any(MessagePostProcessor.class),any(CorrelationData.class));
        jdbc.update("UPDATE notification_outbox SET next_attempt_at=?",LocalDateTime.now().minusSeconds(1)); dispatcher.dispatch(); assertEquals(1,number("SELECT delivered FROM notification_outbox"));
    }
    @Test void cancellingFriendshipWhileSendWaitsRejectsItAfterCommit() throws Exception {
        Long id=chat.open(20L).id(); var started=new CountDownLatch(1); var executor=new ThreadPoolExecutor(1,1,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(1));
        try {
            Future<?>[] future=new Future<?>[1];
            new TransactionTemplate(transactions).executeWithoutResult(status -> {
                jdbc.queryForObject("SELECT id FROM `user` WHERE id=10 FOR UPDATE",Long.class);
                future[0]=executor.submit(() -> { as(10); started.countDown(); assertThrows(BizException.class, () -> send(id,"blocked")); SecurityContextHolder.clearContext(); });
                try { assertTrue(started.await(2,TimeUnit.SECONDS)); } catch(InterruptedException exception) { throw new IllegalStateException(exception); }
                interactions.unfollow(20L);
            });
            future[0].get(5,TimeUnit.SECONDS); assertEquals(0,number("SELECT COUNT(id) FROM chat_message"));
        } finally { executor.shutdownNow(); }
    }
    @Test void clearWhileSendWaitsKeepsNewMessageBeyondCutoff() throws Exception {
        Long id=chat.open(20L).id(); send(id,"old"); var executor=new ThreadPoolExecutor(1,1,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(1));
        try {
            Future<?>[] future=new Future<?>[1];
            new TransactionTemplate(transactions).executeWithoutResult(status -> {
                jdbc.queryForObject("SELECT id FROM chat_conversation WHERE id=? FOR UPDATE",Long.class,id);
                future[0]=executor.submit(() -> { as(20); send(id,"new"); SecurityContextHolder.clearContext(); }); chat.clear(id);
            });
            future[0].get(5,TimeUnit.SECONDS); assertEquals(List.of("new"),chat.history(id,null,null,50).records().stream().map(MessageDtos.Message::content).toList());
        } finally { executor.shutdownNow(); }
    }
}
