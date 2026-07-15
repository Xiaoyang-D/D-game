package com.xiaoyang.d_game.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 当前用户发布管理页的帖子记录。
 *
 * <p>该响应只用于作者自己的管理页，因此同时返回编辑所需元数据、状态和互动统计。</p>
 */
@Data
public class PostManageItemResp {

    @JsonSerialize(using = ToStringSerializer.class)
    /** 帖子 ID。 */
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    /** 所属版区 ID，可为空的草稿允许稍后补充版区。 */
    private Long boardId;

    private String boardName;

    @JsonSerialize(using = ToStringSerializer.class)
    /** 关联游戏 ID。 */
    private Long gameId;

    private String gameName;

    @JsonSerialize(using = ToStringSerializer.class)
    /** 作者选择的个人合集 ID。 */
    private Long collectionId;

    private String collectionName;

    /** 帖子标题。 */
    private String title;

    /** 已清洗的富文本正文，同时用于提取列表缩略图。 */
    private String content;

    /** 内容状态：0 草稿、1 待审核、2 已通过、3 已拒绝。 */
    private Integer status;

    @JsonProperty("isOriginal")
    private Boolean isOriginal;

    private Boolean containsAiGenerated;

    /** 帖子话题。 */
    private List<PostTopicResp> topics;

    /** 浏览次数。 */
    private Integer viewCount;

    /** 点赞次数。 */
    private Integer likeCount;

    /** 评论次数。 */
    private Integer commentCount;

    /** 收藏次数。 */
    private Integer favoriteCount;

    /** 定时进入审核队列的时间。 */
    private LocalDateTime scheduledPublishAt;

    /** 首次创建时间。 */
    private LocalDateTime gmtCreate;

    /** 最近修改时间，管理页草稿列表优先展示该时间。 */
    private LocalDateTime gmtModified;
}
