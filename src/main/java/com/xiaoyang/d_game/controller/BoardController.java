package com.xiaoyang.d_game.controller;

import com.xiaoyang.d_game.common.Result;
import com.xiaoyang.d_game.entity.Board;
import com.xiaoyang.d_game.service.BoardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 社区版块接口。
 *
 * <p>版块是帖子归类的基础数据，通常由初始化脚本或后台维护，前台发帖和筛选列表都会依赖它。</p>
 */
@Tag(name = "版块")
@RestController
@RequestMapping("/api/v1/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;
    private final com.xiaoyang.d_game.service.GameSectionService gameSections;

    /**
     * 查询版块列表。
     *
     * <p>按照 sortOrder 排序返回，方便前端直接按结果顺序渲染导航或筛选项。</p>
     */
    @Operation(summary = "版块列表")
    @GetMapping
    public Result<List<Board>> list(@org.springframework.web.bind.annotation.RequestParam(required = false) Long gameId) {
        return Result.success(gameId == null ? boardService.listBoards() : gameSections.publicBoards(gameId));
    }
}
