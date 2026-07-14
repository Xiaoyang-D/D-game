package com.xiaoyang.d_game.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
/**
 * 点赞/收藏等通用互动请求。
 *
 * <p>targetType 指明目标资源类型，targetId 指明具体资源。</p>
 */
public class InteractReq {

    /** 目标类型：1 帖子，2 评论，3 游戏。 */
    @NotNull(message = "目标类型不能为空")
    private Integer targetType;

    /** 目标资源 ID。 */
    @NotNull(message = "目标ID不能为空")
    private Long targetId;
}
