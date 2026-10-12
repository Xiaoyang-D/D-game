package com.xiaoyang.d_game.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
/**
 * 创建评论请求。
 */
public class CommentCreateReq {
    @Valid
    @Size(max = 50, message = "提及位置最多50处")
    private List<MessageDtos.Mention> mentions;

    /** 被评论的帖子 ID。 */
    @NotNull(message = "帖子ID不能为空")
    private Long postId;

    /** 父评论 ID；0 或空表示顶级评论。 */
    private Long parentId = 0L;

    /** 评论正文，最多 2000 字。 */
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 2000, message = "评论内容不能超过2000")
    private String content;
}
