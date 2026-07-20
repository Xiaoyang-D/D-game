package com.xiaoyang.d_game.controller;

import com.xiaoyang.d_game.common.PageResult;
import com.xiaoyang.d_game.common.Result;
import com.xiaoyang.d_game.dto.GameCreateReq;
import com.xiaoyang.d_game.dto.GameQueryReq;
import com.xiaoyang.d_game.dto.GameRankingQueryReq;
import com.xiaoyang.d_game.dto.GameRatingReq;
import com.xiaoyang.d_game.dto.GameReviewResp;
import com.xiaoyang.d_game.dto.GameResp;
import com.xiaoyang.d_game.entity.GameCategory;
import com.xiaoyang.d_game.entity.Tag;
import com.xiaoyang.d_game.security.RequireLogin;
import org.springframework.security.access.prepost.PreAuthorize;
import com.xiaoyang.d_game.service.GameService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 游戏库接口。
 *
 * <p>前台可分页浏览游戏、查看详情、评分和查看评测；后台管理员可创建游戏。
 * 游戏展示响应会补充分类名称和标签名称，避免前端再发多次基础数据请求。</p>
 */
@io.swagger.v3.oas.annotations.tags.Tag(name = "游戏")
@RestController
@RequestMapping("/api/v1/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    /**
     * 按分类、标签和关键字分页查询游戏。
     */
    @Operation(summary = "分页查询游戏")
    @GetMapping
    public Result<PageResult<GameResp>> page(GameQueryReq req) {
        return Result.success(gameService.pageGames(req));
    }

    /**
     * 查询游戏分类基础数据。
     */
    @Operation(summary = "游戏分类列表")
    @GetMapping("/categories")
    public Result<List<GameCategory>> categories() {
        return Result.success(gameService.listCategories());
    }

    /**
     * 查询游戏标签基础数据。
     */
    @Operation(summary = "标签列表")
    @GetMapping("/tags")
    public Result<List<Tag>> tags() {
        return Result.success(gameService.listTags());
    }

    /**
     * 查询游戏排行榜。
     *
     * <p>支持按综合评分或热度排序，排序策略由 {@link GameRankingQueryReq#getType()} 指定。</p>
     */
    @Operation(summary = "游戏排行榜")
    @GetMapping("/ranking")
    public Result<PageResult<GameResp>> ranking(GameRankingQueryReq req) {
        return Result.success(gameService.pageRanking(req));
    }

    /**
     * 查询游戏详情。
     */
    @Operation(summary = "游戏详情")
    @GetMapping("/{id}")
    public Result<GameResp> detail(@PathVariable Long id) {
        return Result.success(gameService.getGameDetail(id));
    }

    /**
     * 分页查询游戏评测。
     */
    @Operation(summary = "游戏评测列表")
    @GetMapping("/{id}/reviews")
    public Result<PageResult<GameReviewResp>> reviews(@PathVariable Long id,
                                                      @RequestParam(defaultValue = "1") Long page,
                                                      @RequestParam(defaultValue = "10") Long size) {
        return Result.success(gameService.pageGameReviews(id, page, size));
    }

    /**
     * 创建游戏条目。
     *
     * <p>仅管理员可调用，创建游戏时可以同时写入游戏与标签关联。</p>
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "创建游戏")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody GameCreateReq req) {
        return Result.success(gameService.createGame(req));
    }

    /**
     * 当前登录用户对游戏评分/评测。
     *
     * <p>同一用户对同一游戏只保留一条评分记录，重复提交会更新原记录并重新计算游戏平均分。</p>
     */
    @RequireLogin
    @Operation(summary = "评分/评测")
    @PostMapping("/{id}/rating")
    public Result<Void> rate(@PathVariable Long id, @Valid @RequestBody GameRatingReq req) {
        gameService.rateGame(id, req);
        return Result.success();
    }
}
