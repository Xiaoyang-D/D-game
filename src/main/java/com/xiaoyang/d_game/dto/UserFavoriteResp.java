package com.xiaoyang.d_game.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
/**
 * 公开收藏流响应。
 *
 * <p>这是一个统一 DTO，同一条收藏记录既可以表示帖子，也可以表示游戏。
 * 前端根据 {@code targetType} 区分展示样式和跳转地址。</p>
 */
public class UserFavoriteResp {

    /** 收藏目标类型。 */
    private Integer targetType;

    /** 收藏目标 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long targetId;

    /** 目标标题；帖子是帖子标题，游戏是游戏名称。 */
    private String title;

    /** 目标正文或简介；帖子是富文本内容，游戏是简介。 */
    private String content;

    /** 封面图，游戏会返回封面，帖子一般为空。 */
    private String coverUrl;

    /** 帖子的版块 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long boardId;

    /** 帖子的版块名称。 */
    private String boardName;

    /** 帖子关联的游戏 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long gameId;

    /** 帖子关联的游戏名称。 */
    private String gameName;

    /** 帖子的作者 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    /** 帖子的作者昵称。 */
    private String authorNickname;

    /** 帖子的作者用户名。 */
    private String authorUsername;

    /** 帖子的作者头像。 */
    private String authorAvatarUrl;

    /** 帖子的作者状态。 */
    private Integer authorStatus;

    /** 帖子审核状态。 */
    private Integer status;

    /** 浏览数。 */
    private Integer viewCount;

    /** 点赞数。 */
    private Integer likeCount;

    /** 评论数。 */
    private Integer commentCount;

    /** 收藏数。 */
    private Integer favoriteCount;

    /** 游戏分类名称。 */
    private String categoryName;

    /** 游戏平均分。 */
    private BigDecimal avgRating;

    /** 游戏评分人数。 */
    private Integer ratingCount;

    /** 收藏时间。 */
    private LocalDateTime gmtCreate;
}
