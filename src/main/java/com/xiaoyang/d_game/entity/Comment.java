package com.xiaoyang.d_game.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoyang.d_game.common.BaseEntity;
import com.xiaoyang.d_game.common.enums.ContentStatusEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("comment")
/**
 * 评论实体。
 *
 * <p>评论挂在帖子下，支持父评论 ID 表达楼中楼结构。</p>
 */
public class Comment extends BaseEntity {

    /** 所属帖子 ID。 */
    private Long postId;

    /** 评论作者用户 ID。 */
    private Long userId;

    /** 父评论 ID；0 表示顶级评论。 */
    private Long parentId = 0L;

    /** 评论正文，长度限制由数据库和请求 DTO 共同约束。 */
    private String content;

    /** 评论被点赞次数。 */
    private Integer likeCount = 0;

    /** 评论审核状态，取值见 {@link ContentStatusEnum}。 */
    private Integer status = ContentStatusEnum.APPROVED.getCode();
}
