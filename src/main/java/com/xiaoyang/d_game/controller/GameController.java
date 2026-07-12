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
import com.xiaoyang.d_game.security.RequireRole;
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

@io.swagger.v3.oas.annotations.tags.Tag(name = "游戏")
@RestController
@RequestMapping("/api/v1/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    @Operation(summary = "分页查询游戏")
    @GetMapping
    public Result<PageResult<GameResp>> page(GameQueryReq req) {
        return Result.success(gameService.pageGames(req));
    }

    @Operation(summary = "游戏分类列表")
    @GetMapping("/categories")
    public Result<List<GameCategory>> categories() {
        return Result.success(gameService.listCategories());
    }

    @Operation(summary = "标签列表")
    @GetMapping("/tags")
    public Result<List<Tag>> tags() {
        return Result.success(gameService.listTags());
    }

    @Operation(summary = "游戏排行榜")
    @GetMapping("/ranking")
    public Result<PageResult<GameResp>> ranking(GameRankingQueryReq req) {
        return Result.success(gameService.pageRanking(req));
    }

    @Operation(summary = "游戏详情")
    @GetMapping("/{id}")
    public Result<GameResp> detail(@PathVariable Long id) {
        return Result.success(gameService.getGameDetail(id));
    }

    @Operation(summary = "游戏评测列表")
    @GetMapping("/{id}/reviews")
    public Result<PageResult<GameReviewResp>> reviews(@PathVariable Long id,
                                                      @RequestParam(defaultValue = "1") Long page,
                                                      @RequestParam(defaultValue = "10") Long size) {
        return Result.success(gameService.pageGameReviews(id, page, size));
    }

    @RequireRole("ADMIN")
    @Operation(summary = "创建游戏")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody GameCreateReq req) {
        return Result.success(gameService.createGame(req));
    }

    @RequireLogin
    @Operation(summary = "评分/评测")
    @PostMapping("/{id}/rating")
    public Result<Void> rate(@PathVariable Long id, @Valid @RequestBody GameRatingReq req) {
        gameService.rateGame(id, req);
        return Result.success();
    }
}
