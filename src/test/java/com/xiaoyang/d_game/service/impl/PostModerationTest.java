package com.xiaoyang.d_game.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaoyang.d_game.common.BizException;
import com.xiaoyang.d_game.common.HtmlSanitizer;
import com.xiaoyang.d_game.dto.AuditReq;
import com.xiaoyang.d_game.dto.PostCreateReq;
import com.xiaoyang.d_game.dto.PostPublishReq;
import com.xiaoyang.d_game.dto.PostQueryReq;
import com.xiaoyang.d_game.entity.AuditLog;
import com.xiaoyang.d_game.entity.Board;
import com.xiaoyang.d_game.entity.Post;
import com.xiaoyang.d_game.entity.PostTopicRel;
import com.xiaoyang.d_game.mapper.*;
import com.xiaoyang.d_game.security.CurrentUserPrincipal;
import com.xiaoyang.d_game.service.GameSectionService;
import com.xiaoyang.d_game.service.MentionService;
import com.xiaoyang.d_game.service.NotificationService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.*;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PostModerationTest {
    private static MentionService mentions() {
        return mock(MentionService.class, invocation ->
                invocation.getMethod().getName().equals("normalizePost") ? invocation.getArgument(0)
                        : Answers.RETURNS_DEFAULTS.answer(invocation));
    }

    private PostMapper posts;
    private AuditLogMapper logs;
    private NotificationService notifications;
    private AdminServiceImpl admin;

    private GameSectionService policyService() {
        return new GameSectionService(mock(GameMapper.class), mock(BoardMapper.class),
                mock(GameBoardSettingMapper.class), mock(GameCategoryMapper.class));
    }

    @BeforeEach
    void setup() {
        Configuration configuration = new Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, "post-test"), Post.class);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new CurrentUserPrincipal(10L, "admin"), null, List.of()));
        posts = mock(PostMapper.class);
        logs = mock(AuditLogMapper.class);
        notifications = mock(NotificationService.class);
        admin = new AdminServiceImpl(mock(UserMapper.class), posts, mock(CommentMapper.class),
                mock(SysRoleMapper.class), mock(UserRoleRelMapper.class), logs, notifications, mentions());
    }

    @AfterEach
    void cleanup() { SecurityContextHolder.clearContext(); }

    private void prepare(int status, boolean approved) {
        Post post = new Post();
        post.setId(20L);
        post.setUserId(30L);
        post.setStatus(status);
        when(posts.selectById(20L)).thenReturn(post);
        when(posts.selectOne(any(Wrapper.class))).thenReturn(post);
    }

    private AuditReq request(boolean approved) {
        AuditReq request = new AuditReq();
        request.setApproved(approved);
        request.setReason("违规内容");
        return request;
    }

    @Test
    void publishedPostCanBeBannedAndAuthorIsNotified() {
        prepare(2, false);
        when(posts.update(isNull(), any(Wrapper.class))).thenReturn(1);
        admin.auditPost(20L, request(false));
        var log = ArgumentCaptor.forClass(AuditLog.class);
        verify(logs).insert(log.capture());
        assertEquals("BAN_POST", log.getValue().getAction());
        verify(notifications).sendNotification(eq(30L), eq(10L), anyInt(), anyString(), contains("封禁"), anyInt(), eq(20L));
    }

    @Test
    void bannedPostCanBeRestored() {
        prepare(3, true);
        when(posts.update(isNull(), any(Wrapper.class))).thenReturn(1);
        admin.auditPost(20L, request(true));
        var log = ArgumentCaptor.forClass(AuditLog.class);
        verify(logs).insert(log.capture());
        assertEquals("UNBAN_POST", log.getValue().getAction());
    }

    @Test
    void repeatedBanDoesNotDuplicateNotification() {
        prepare(3, false);
        admin.auditPost(20L, request(false));
        verifyNoInteractions(logs, notifications);
        verify(posts, never()).update(isNull(), any(Wrapper.class));
    }

    @Test
    void draftCannotBePublishedByAdmin() {
        prepare(0, true);
        assertThrows(BizException.class, () -> admin.auditPost(20L, request(true)));
        verifyNoInteractions(logs, notifications);
    }

    @Test
    void concurrentStatusChangeDoesNotWriteMisleadingAudit() {
        prepare(2, false);
        when(posts.update(isNull(), any(Wrapper.class))).thenReturn(0);
        assertThrows(BizException.class, () -> admin.auditPost(20L, request(false)));
        verifyNoInteractions(logs, notifications);
    }

    @Test
    void authorEditCannotRestoreBannedPost() {
        BoardMapper boards = mock(BoardMapper.class);
        when(boards.selectById(1L)).thenReturn(new Board());
        HtmlSanitizer sanitizer = mock(HtmlSanitizer.class);
        when(sanitizer.sanitizePostContent(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        PostTopicRelMapper relations = mock(PostTopicRelMapper.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new Configuration(), "topic-test"),
                PostTopicRel.class);
        PostServiceImpl service = spy(new PostServiceImpl(policyService(), boards, mock(GameMapper.class), mock(UserMapper.class),
                mock(UserFollowMapper.class), sanitizer, mock(PostTopicMapper.class), relations, mock(PostCollectionMapper.class), mentions()));
        Post post = new Post();
        post.setId(20L);
        post.setUserId(10L);
        post.setStatus(3);
        doReturn(post).when(service).getById(20L);
        doReturn(true).when(service).update(any(Post.class), any(Wrapper.class));
        var request = new PostPublishReq();
        request.setBoardId(1L);
        request.setTitle("编辑帖子");
        request.setContent("修改正文");
        assertEquals(3, service.updateMyManagedPost(20L, request).getStatus());
        doReturn(false).when(service).update(any(Post.class), any(Wrapper.class));
        assertThrows(BizException.class, () -> service.updateMyManagedPost(20L, request));
    }

    @Test
    void recommendationKeepsGameAndSectionFilters() {
        PostServiceImpl service = spy(new PostServiceImpl(policyService(), mock(BoardMapper.class), mock(GameMapper.class),
                mock(UserMapper.class), mock(UserFollowMapper.class), mock(HtmlSanitizer.class),
                mock(PostTopicMapper.class), mock(PostTopicRelMapper.class), mock(PostCollectionMapper.class), mentions()));
        doReturn(new Page<Post>(1, 10))
                .when(service).page(any(IPage.class), any(Wrapper.class));
        var request = new PostQueryReq();
        request.setGameId(100L);
        request.setBoardId(2L);
        request.setRecommended(true);
        service.pagePosts(request);
        var wrapper = ArgumentCaptor.forClass(Wrapper.class);
        verify(service).page(any(IPage.class), wrapper.capture());
        String sql = wrapper.getValue().getSqlSegment();
        assertTrue(sql.contains("game_id"));
        assertTrue(sql.contains("board_id"));
        assertTrue(sql.contains("status"));
        assertTrue(sql.contains("like_count DESC"));
    }

    @Test
    void replySortingKeepsScopeAndExcludesHiddenComments() {
        PostServiceImpl service = spy(new PostServiceImpl(policyService(), mock(BoardMapper.class), mock(GameMapper.class),
                mock(UserMapper.class), mock(UserFollowMapper.class), mock(HtmlSanitizer.class),
                mock(PostTopicMapper.class), mock(PostTopicRelMapper.class), mock(PostCollectionMapper.class), mentions()));
        doReturn(new Page<Post>(1, 10))
                .when(service).page(any(IPage.class), any(Wrapper.class));
        var request = new PostQueryReq();
        request.setGameId(100L);
        request.setBoardId(2L);
        request.setSort("LATEST_REPLY");
        service.pagePosts(request);
        var wrapper = ArgumentCaptor.forClass(Wrapper.class);
        verify(service).page(any(IPage.class), wrapper.capture());
        String sql = wrapper.getValue().getSqlSegment();
        assertTrue(sql.contains("game_id"));
        assertTrue(sql.contains("board_id"));
        assertTrue(sql.contains("MAX(c.gmt_create)"));
        assertTrue(sql.contains("c.is_deleted = 0 AND c.status = 2"));
        assertTrue(sql.contains("post.gmt_create) DESC"));
        assertFalse(sql.contains("like_count DESC"));
    }

    @Test
    void ordinaryUserCannotPublishOfficialPost() {
        BoardMapper boards = mock(BoardMapper.class);
        Board official = new Board();
        official.setName("官方"); official.setPublishPolicy("ADMIN");
        when(boards.selectById(1L)).thenReturn(official);
        PostServiceImpl service = spy(new PostServiceImpl(policyService(), boards, mock(GameMapper.class), mock(UserMapper.class),
                mock(UserFollowMapper.class), mock(HtmlSanitizer.class), mock(PostTopicMapper.class),
                mock(PostTopicRelMapper.class), mock(PostCollectionMapper.class), mentions()));
        PostCreateReq request = new PostCreateReq();
        request.setBoardId(1L);
        request.setTitle("冒充官方");
        request.setContent("正文");
        assertThrows(BizException.class, () -> service.createPost(request));
        verify(service, never()).save(any(Post.class));
    }

    @Test
    void newPostIsImmediatelyPublic() {
        BoardMapper boards = mock(BoardMapper.class);
        when(boards.selectById(1L)).thenReturn(new Board());
        HtmlSanitizer sanitizer = mock(HtmlSanitizer.class);
        when(sanitizer.sanitizePostContent(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        PostServiceImpl service = spy(new PostServiceImpl(policyService(), boards, mock(GameMapper.class), mock(UserMapper.class),
                mock(UserFollowMapper.class), sanitizer, mock(PostTopicMapper.class), mock(PostTopicRelMapper.class), mock(PostCollectionMapper.class), mentions()));
        doReturn(true).when(service).save(any(Post.class));
        PostCreateReq request = new PostCreateReq();
        request.setBoardId(1L);
        request.setTitle("测试帖子");
        request.setContent("正文");
        service.createPost(request);
        var saved = ArgumentCaptor.forClass(Post.class);
        verify(service).save(saved.capture());
        assertEquals(2, saved.getValue().getStatus());
    }
}
