package com.xiaoyang.d_game.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BatchAuditReq {

    @NotEmpty(message = "帖子ID列表不能为空")
    private List<String> postIds;

    @NotNull(message = "是否通过不能为空")
    private Boolean approved;

    private String reason;
}
