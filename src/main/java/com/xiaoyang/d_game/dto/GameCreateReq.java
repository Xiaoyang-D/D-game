package com.xiaoyang.d_game.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class GameCreateReq {

    @NotBlank(message = "游戏名称不能为空")
    private String name;

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    private String coverUrl;

    private String description;

    private String developer;

    private LocalDate releaseDate;

    private List<Long> tagIds;
}
