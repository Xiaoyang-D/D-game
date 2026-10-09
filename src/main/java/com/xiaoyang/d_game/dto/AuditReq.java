package com.xiaoyang.d_game.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
/**
 * 单条内容审核请求。
 */
public class AuditReq {

    /** true 表示审核通过，false 表示审核拒绝。 */
    @NotNull(message = "是否通过不能为空")
    private Boolean approved;

    /** 审核原因或备注，可用于后台记录。 */
    @Size(max = 500, message = "操作原因最多500字")
    private String reason;
}
