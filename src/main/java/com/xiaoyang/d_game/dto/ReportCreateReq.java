package com.xiaoyang.d_game.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
/**
 * 创建举报请求。
 */
public class ReportCreateReq {

    /** 举报目标类型：1 帖子，2 评论，3 游戏。 */
    @NotNull
    private Integer targetType;

    /** 举报目标 ID。 */
    @NotNull
    private Long targetId;

    /** 举报原因，最多 500 字。 */
    @NotBlank
    @Size(max = 500)
    private String reason;
}
