package com.xiaoyang.d_game.controller;

import com.xiaoyang.d_game.common.PageResult;
import com.xiaoyang.d_game.common.Result;
import com.xiaoyang.d_game.dto.GameBoardSaveReq;
import com.xiaoyang.d_game.dto.GameSectionSaveReq;
import com.xiaoyang.d_game.entity.Game;
import com.xiaoyang.d_game.entity.GameBoardSetting;
import com.xiaoyang.d_game.service.GameSectionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/** 版区及分区管理，只允许管理员访问。 */
@RestController
@RequestMapping("/api/v1/admin/game-sections")
@PreAuthorize("hasRole('ADMIN')")
@Validated
@RequiredArgsConstructor
public class GameSectionController {
    private final GameSectionService service;
    @GetMapping
    public Result<PageResult<Game>> page(@RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        return Result.success(service.page(page, size));
    }
    @GetMapping("/{gameId}")
    public Result<Game> detail(@PathVariable Long gameId) { return Result.success(service.requireGame(gameId)); }
    @PostMapping
    public Result<Long> create(@Valid @RequestBody GameSectionSaveReq req) { return Result.success(service.create(req)); }
    @PutMapping("/{gameId}")
    public Result<Void> update(@PathVariable Long gameId, @Valid @RequestBody GameSectionSaveReq req) {
        service.update(gameId, req); return Result.success();
    }
    @GetMapping("/{gameId}/boards")
    public Result<List<GameBoardSetting>> boards(@PathVariable Long gameId) { return Result.success(service.listSettings(gameId, true)); }
    @PostMapping("/{gameId}/boards")
    public Result<Long> createBoard(@PathVariable Long gameId, @Valid @RequestBody GameBoardSaveReq req) {
        return Result.success(service.createBoard(gameId, req));
    }
    @PutMapping("/{gameId}/boards/{boardId}")
    public Result<Void> updateBoard(@PathVariable Long gameId, @PathVariable Long boardId, @Valid @RequestBody GameBoardSaveReq req) {
        service.updateBoard(gameId, boardId, req); return Result.success();
    }
}
