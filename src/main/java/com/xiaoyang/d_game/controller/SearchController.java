package com.xiaoyang.d_game.controller;

import com.xiaoyang.d_game.common.Result;
import com.xiaoyang.d_game.dto.SearchQueryReq;
import com.xiaoyang.d_game.dto.SearchResp;
import com.xiaoyang.d_game.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 统一搜索接口。
 *
 * <p>根据查询类型同时或分别搜索游戏和帖子。公开搜索只返回可公开展示的数据，
 * 例如帖子只查审核通过的内容。</p>
 */
@Tag(name = "搜索")
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    /**
     * 搜索游戏与帖子。
     *
     * <p>关键词为空时由服务层返回空结果，避免无意义地扫描全量内容。</p>
     */
    @Operation(summary = "统一搜索游戏与帖子")
    @GetMapping
    public Result<SearchResp> search(SearchQueryReq req) {
        return Result.success(searchService.search(req));
    }
}
