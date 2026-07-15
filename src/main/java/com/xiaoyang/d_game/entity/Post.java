package com.xiaoyang.d_game.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.xiaoyang.d_game.common.BaseEntity;
import com.xiaoyang.d_game.common.enums.ContentStatusEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("post")
/**
 * 帖子实体。
 *
 * <p>帖子属于某个版块，可选关联一个游戏；发帖后默认待审核，通过后进入公开列表。</p>
 */
public class Post extends BaseEntity {

    /** 所属版块 ID。 */
    private Long boardId;

    /** 关联游戏 ID，可为空。 */
    private Long gameId;

    /** Personal collection selected by the author. */
    private Long collectionId;

    /** 作者用户 ID。 */
    private Long userId;

    /** 帖子标题。 */
    private String title;

    /** 帖子正文，允许清洗后的富文本 HTML。 */
    private String content;

    private Boolean isOriginal = false;

    private Boolean containsAiGenerated = false;

    /** 帖子状态，取值见 {@link ContentStatusEnum}。 */
    private Integer status = ContentStatusEnum.PENDING.getCode();

    private java.time.LocalDateTime scheduledPublishAt;

    /** 浏览次数。 */
    private Integer viewCount = 0;

    /** 点赞次数。 */
    private Integer likeCount = 0;

    /** 评论次数。 */
    private Integer commentCount = 0;

    /** 收藏次数。 */
    private Integer favoriteCount = 0;

    /** 乐观锁版本号，用于并发更新保护。 */
    @Version
    private Integer version = 0;
}
