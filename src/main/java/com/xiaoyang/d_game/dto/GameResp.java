package com.xiaoyang.d_game.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
/**
 * 游戏响应。
 *
 * <p>相比 Game 实体，额外补充分类名称和标签名称，便于前端直接展示。</p>
 */
public class GameResp {

    /** 游戏 ID。 */
    private Long id;

    /** 游戏名称。 */
    private String name;

    /** 分类 ID。 */
    private Long categoryId;

    /** 分类名称。 */
    private String categoryName;

    /** 封面 URL。 */
    private String coverUrl;

    /** 游戏简介。 */
    private String description;

    /** 开发商。 */
    private String developer;

    /** 发行日期。 */
    private LocalDate releaseDate;

    /** 平均评分。 */
    private BigDecimal avgRating;

    /** 评分人数。 */
    private Integer ratingCount;

    /** 标签名称列表。 */
    private List<String> tags;
}
