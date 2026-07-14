package com.xiaoyang.d_game.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
/**
 * 创建游戏请求。
 *
 * <p>仅管理员接口使用，创建主表数据时可同步绑定标签。</p>
 */
public class GameCreateReq {

    /** 游戏名称。 */
    @NotBlank(message = "游戏名称不能为空")
    private String name;

    /** 游戏分类 ID。 */
    @NotNull(message = "分类不能为空")
    private Long categoryId;

    /** 封面图片 URL。 */
    private String coverUrl;

    /** 游戏简介。 */
    private String description;

    /** 开发商或制作团队。 */
    private String developer;

    /** 发行日期。 */
    private LocalDate releaseDate;

    /** 绑定的标签 ID 列表。 */
    private List<Long> tagIds;
}
