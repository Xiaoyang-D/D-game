package com.xiaoyang.d_game.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PostCollectionCreateReq {

    @NotBlank(message = "合集名称不能为空")
    @Size(max = 64, message = "合集名称不能超过64个字符")
    private String name;
}
