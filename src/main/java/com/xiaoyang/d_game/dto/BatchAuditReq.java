package com.xiaoyang.d_game.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
/**
 * 批量审核帖子请求。
 */
public class BatchAuditReq {

    /** 待审核帖子 ID 列表，前端以字符串传递以兼容大整数 ID。 */
    @NotEmpty(message = "帖子ID列表不能为空")
    private List<String> postIds;

    /** true 表示批量通过，false 表示批量拒绝。 */
    @NotNull(message = "是否通过不能为空")
    private Boolean approved;

    /** 批量审核原因或备注。 */
    private String reason;
}
