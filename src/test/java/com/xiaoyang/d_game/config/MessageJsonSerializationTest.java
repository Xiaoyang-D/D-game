package com.xiaoyang.d_game.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoyang.d_game.controller.ChatController;
import com.xiaoyang.d_game.controller.MentionController;
import com.xiaoyang.d_game.controller.MessageSummaryController;
import com.xiaoyang.d_game.dto.MessageDtos;
import com.xiaoyang.d_game.service.ChatService;
import com.xiaoyang.d_game.service.MentionService;
import com.xiaoyang.d_game.service.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.json.SpringHandlerInstantiator;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 验证 HTTP 响应使用真实 Spring 序列化器实例化路径及项目 Long 转字符串配置。 */
class MessageJsonSerializationTest {
    private static final long CONVERSATION_ID = 2062801749048246274L;
    private static final long FRIEND_ID = 2062801749048246275L;
    private AnnotationConfigApplicationContext context;
    private ObjectMapper json;
    private ChatService chat;
    private MentionService mentions;
    private NotificationService notifications;
    private MockMvc mvc;

    @BeforeEach
    void initialize() {
        context = new AnnotationConfigApplicationContext(JacksonConfig.class);
        Jackson2ObjectMapperBuilder builder = Jackson2ObjectMapperBuilder.json().applicationContext(context);
        context.getBean(Jackson2ObjectMapperBuilderCustomizer.class).customize(builder);
        json = builder.build();
        assertInstanceOf(SpringHandlerInstantiator.class, json.getSerializationConfig().getHandlerInstantiator());
        chat = mock(ChatService.class);
        mentions = mock(MentionService.class);
        notifications = mock(NotificationService.class);
        mvc = MockMvcBuilders.standaloneSetup(new ChatController(chat), new MentionController(mentions),
                        new MessageSummaryController(chat, notifications))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(json))
                .build();
    }

    @AfterEach
    void closeContext() {
        context.close();
    }

    @Test
    void openingConversationReturnsStringIdsAndNumericUnreadCount() throws Exception {
        when(chat.open(FRIEND_ID)).thenReturn(new MessageDtos.Conversation(CONVERSATION_ID, FRIEND_ID,
                "测试用户", null, null, null, 5L, true));

        JsonNode data = data(mvc.perform(post("/api/v1/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"friendId\":\"" + FRIEND_ID + "\"}"))
                .andExpect(status().isOk()).andReturn());

        assertStringId(data, "id", CONVERSATION_ID);
        assertStringId(data, "friendId", FRIEND_ID);
        assertNumber(data, "unreadCount", 5L);
        assertTrue(data.get("isFriend").booleanValue());
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, 2147483648L})
    void unreadSummaryReturnsNumericCategoriesWithoutNarrowingLongs(long privateMessages) throws Exception {
        when(chat.unread()).thenReturn(privateMessages);
        when(notifications.categoryUnread("reply")).thenReturn(1L);
        when(notifications.categoryUnread("mention")).thenReturn(2L);
        when(notifications.categoryUnread("like")).thenReturn(3L);
        when(notifications.categoryUnread("system")).thenReturn(4L);
        when(notifications.categoryUnread("social")).thenReturn(5L);

        JsonNode data = data(mvc.perform(get("/api/v1/messages/unread-summary"))
                .andExpect(status().isOk()).andReturn());

        assertNumber(data, "total", privateMessages + 15L);
        assertNumber(data, "privateMessages", privateMessages);
        assertNumber(data, "replies", 1L);
        assertNumber(data, "mentions", 2L);
        assertNumber(data, "likes", 3L);
        assertNumber(data, "system", 4L);
        assertNumber(data, "social", 5L);
    }

    @Test
    void commentLocationReturnsNumericPageAndStringTargetIds() throws Exception {
        when(mentions.location(CONVERSATION_ID, "DEFAULT", 20))
                .thenReturn(new MessageDtos.Location(FRIEND_ID, CONVERSATION_ID, 4L));

        JsonNode data = data(mvc.perform(get("/api/v1/comments/{id}/location", CONVERSATION_ID))
                .andExpect(status().isOk()).andReturn());

        assertStringId(data, "postId", FRIEND_ID);
        assertStringId(data, "commentId", CONVERSATION_ID);
        assertNumber(data, "page", 4L);
    }

    @Test
    void messageIdsAndSequenceKeepTheirStringRepresentation() throws Exception {
        LocalDateTime sentAt = LocalDateTime.of(2026, 10, 11, 22, 30);
        when(chat.send(eq(CONVERSATION_ID), any(MessageDtos.Send.class))).thenReturn(new MessageDtos.Message(
                FRIEND_ID, CONVERSATION_ID, FRIEND_ID, Long.MAX_VALUE, "你好 😀", false,
                sentAt, sentAt.plusMinutes(2)));

        JsonNode data = data(mvc.perform(post("/api/v1/conversations/{id}/messages", CONVERSATION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"你好 😀\",\"clientMessageId\":\"ba125cb7-a2c4-4fb1-8d55-d54a7a43b96c\"}"))
                .andExpect(status().isOk()).andReturn());

        assertStringId(data, "id", FRIEND_ID);
        assertStringId(data, "conversationId", CONVERSATION_ID);
        assertStringId(data, "senderId", FRIEND_ID);
        assertStringId(data, "seq", Long.MAX_VALUE);
        assertEquals("你好 😀", data.get("content").textValue());
    }

    private JsonNode data(MvcResult response) throws Exception {
        JsonNode envelope = json.readTree(response.getResponse().getContentAsByteArray());
        assertEquals(0, envelope.get("code").intValue());
        return envelope.get("data");
    }

    private static void assertStringId(JsonNode data, String field, long expected) {
        assertTrue(data.get(field).isTextual(), field);
        assertEquals(Long.toString(expected), data.get(field).textValue());
    }

    private static void assertNumber(JsonNode data, String field, long expected) {
        assertTrue(data.get(field).isIntegralNumber(), field);
        assertEquals(expected, data.get(field).longValue());
    }
}
