package com.xiaoyang.d_game.dto;

import lombok.Data;

@Data
/**
 * 当前用户对指定目标的互动状态响应。
 *
 * <p>用于前端在刷新页面或重新进入详情页时恢复点赞、收藏按钮状态。
 * Redis 只作为加速缓存，数据库关系表仍然是最终事实来源。</p>
 */
public class InteractionStatusResp {

    /** 当前用户是否已经点赞该目标。 */
    private boolean liked;

    /** 当前用户是否已经收藏该目标。 */
    private boolean favorited;
}
