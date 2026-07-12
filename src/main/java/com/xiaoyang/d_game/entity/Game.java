package com.xiaoyang.d_game.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoyang.d_game.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("game")
public class Game extends BaseEntity {

    private String name;

    private Long categoryId;

    private String coverUrl;

    private String description = "";

    private String developer = "";

    private LocalDate releaseDate;

    private BigDecimal avgRating = BigDecimal.ZERO;

    private Integer ratingCount = 0;
}
