package com.xiaoyang.d_game.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
/**
 * 帖子响应。
 *
 * <p>相比 Post 实体，额外补充版块名、游戏名和作者展示信息。</p>
 */
public class PostResp {

    /** 帖子 ID，序列化为字符串避免前端精度丢失。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 版块 ID，序列化为字符串避免前端精度丢失。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long boardId;

    /** 版块名称。 */
    private String boardName;

    /** 关联游戏 ID，可能为空。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long gameId;

    /** 关联游戏名称。 */
    private String gameName;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long collectionId;

    private String collectionName;

    /** 作者用户 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    /** 作者昵称。 */
    private String authorNickname;

    /** 作者用户名。 */
    private String authorUsername;

    /** 作者头像 URL。 */
    private String authorAvatarUrl;

    /** 作者状态：0 封禁 / 1 正常 */
    private Integer authorStatus;

    /** 帖子标题。 */
    private String title;

    /** 帖子正文，已经过 HTML 安全清洗。 */
    private String content;

    private List<PostTopicResp> topics;

    @JsonProperty("isOriginal")
    private boolean isOriginal;

    private boolean containsAiGenerated;

    /** 帖子审核状态。 */
    private Integer status;

    private LocalDateTime scheduledPublishAt;

    /** 浏览数。 */
    private Integer viewCount;

    /** 点赞数。 */
    private Integer likeCount;

    /** 评论数。 */
    private Integer commentCount;

    /** 收藏数。 */
    private Integer favoriteCount;

    /** 创建时间。 */
    private LocalDateTime gmtCreate;
}
