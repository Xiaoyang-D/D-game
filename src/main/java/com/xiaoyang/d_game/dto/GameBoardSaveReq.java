package com.xiaoyang.d_game.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 游戏内部分类编辑请求。 */
@Data
public class GameBoardSaveReq {
    @NotBlank @Size(max = 64)
    private String name;
    @NotNull @Size(max = 2000)
    private String description = "";
    @NotNull @Pattern(regexp = "forum|official|guide|help|art|camera")
    private String iconKey = "forum";
    @NotNull @Size(max = 1024) @Pattern(regexp = "^(?:$|https?://[^\\s]+|/(?!/)[^\\s]*)$")
    private String iconUrl = "";
    @NotNull @Size(max = 1024) @Pattern(regexp = "^(?:$|https?://[^\\s]+|/(?!/)[^\\s]*)$")
    private String bannerUrl = "";
    @NotNull @Min(0) @Max(1000000)
    private Integer sortOrder = 0;
    @NotNull
    private Boolean enabled = true;
    @NotNull @Pattern(regexp = "LOGIN|ADMIN")
    private String publishPolicy = "LOGIN";
}
