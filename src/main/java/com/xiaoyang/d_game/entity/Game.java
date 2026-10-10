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
/**
 * 游戏实体。
 *
 * <p>游戏库的主表数据，包含分类、封面、简介、开发商、发行日期和评分汇总字段。</p>
 */
public class Game extends BaseEntity {

    /** 游戏名称。 */
    private String name;

    /** 游戏分类 ID，对应 game_category 表。 */
    private Long categoryId;

    /** 游戏封面图片 URL。 */
    private String coverUrl;

    /** 游戏简介。 */
    private String description = "";

    /** 开发商或制作团队。 */
    private String developer = "";

    /** 发行日期，可为空。 */
    private LocalDate releaseDate;

    /** 平均评分，来自 game_rating 表汇总，保留两位小数。 */
    private BigDecimal avgRating = BigDecimal.ZERO;

    /** 评分人数，来自 game_rating 表汇总。 */
    private Integer ratingCount = 0;
    /** 游戏版区展示设置，与封面分开维护。 */
    private String englishName = "";
    private String iconUrl = "";
    private String bannerUrl = "";
    private Integer sortOrder = 0;
    private Boolean enabled = true;
}
