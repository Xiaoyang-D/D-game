package com.xiaoyang.d_game.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 游戏版区完整编辑请求，空字符串表示清除图片。 */
@Data
public class GameSectionSaveReq {
    @NotBlank @Size(max = 64)
    private String name;
    @NotNull @Size(max = 128)
    private String englishName = "";
    @NotNull @Size(max = 1000)
    private String description = "";
    @NotNull @Size(max = 1024) @Pattern(regexp = "^(?:$|https?://[^\\s]+|/(?!/)[^\\s]*)$")
    private String iconUrl = "";
    @NotNull @Size(max = 1024) @Pattern(regexp = "^(?:$|https?://[^\\s]+|/(?!/)[^\\s]*)$")
    private String bannerUrl = "";
    @NotNull @Min(0) @Max(1000000)
    private Integer sortOrder = 0;
    @NotNull
    private Boolean enabled = true;
    private Long categoryId;
}
