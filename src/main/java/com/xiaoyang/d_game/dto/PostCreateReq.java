package com.xiaoyang.d_game.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
/**
 * 创建帖子请求。
 */
public class PostCreateReq {

    /** 所属版块 ID。 */
    @NotNull(message = "版块不能为空")
    private Long boardId;

    /** 关联游戏 ID，可为空。 */
    private Long gameId;

    /** 帖子标题，最多 200 字。 */
    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题长度不能超过200")
    private String title;

    /** 帖子正文，允许富文本 HTML，服务层会做安全清洗。 */
    @NotBlank(message = "正文不能为空")
    private String content;
}
