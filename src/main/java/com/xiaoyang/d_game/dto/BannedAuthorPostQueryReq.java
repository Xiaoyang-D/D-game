package com.xiaoyang.d_game.dto;

import lombok.Data;

@Data
/**
 * 封禁作者帖子查询请求。
 *
 * <p>后台用于复查被封禁用户发布过的帖子。</p>
 */
public class BannedAuthorPostQueryReq {

    /** 指定封禁作者 ID，可为空。 */
    private Long authorId;

    /** 作者昵称或用户名关键字。 */
    private String keyword;

    /** 页码，从 1 开始。 */
    private Long page = 1L;

    /** 每页条数。 */
    private Long size = 10L;
}
