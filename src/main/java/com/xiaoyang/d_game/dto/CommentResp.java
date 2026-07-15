package com.xiaoyang.d_game.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
/**
 * 评论响应。
 */
public class CommentResp {

    /** 评论 ID。 */
    private Long id;

    /** 所属帖子 ID。 */
    private Long postId;

    /** 父评论 ID；0 表示顶级评论。 */
    private Long parentId;

    /** 评论作者 ID。 */
    private Long userId;

    /** 评论作者昵称。 */
    private String userNickname;

    private String userAvatarUrl;

    /** 评论正文。 */
    private String content;

    /** 点赞数。 */
    private Integer likeCount;

    /** 评论创建时间。 */
    private LocalDateTime gmtCreate;
}
