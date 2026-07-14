package com.xiaoyang.d_game.service;

import com.xiaoyang.d_game.dto.SearchQueryReq;
import com.xiaoyang.d_game.dto.SearchResp;

/**
 * 统一搜索服务接口。
 *
 * <p>根据搜索类型查询游戏、帖子或二者合集。公开搜索只返回允许前台展示的数据。</p>
 */
public interface SearchService {

    /**
     * 执行统一搜索。
     */
    SearchResp search(SearchQueryReq req);
}
