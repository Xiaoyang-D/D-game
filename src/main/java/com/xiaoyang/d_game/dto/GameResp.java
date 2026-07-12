package com.xiaoyang.d_game.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class GameResp {

    private Long id;

    private String name;

    private Long categoryId;

    private String categoryName;

    private String coverUrl;

    private String description;

    private String developer;

    private LocalDate releaseDate;

    private BigDecimal avgRating;

    private Integer ratingCount;

    private List<String> tags;
}
