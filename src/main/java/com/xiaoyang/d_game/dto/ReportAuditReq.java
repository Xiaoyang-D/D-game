package com.xiaoyang.d_game.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
/**
 * 举报处理请求。
 */
public class ReportAuditReq {

    /** true 表示已处理，false 表示驳回举报。 */
    @NotNull
    private Boolean handled;

    /** 管理员处理备注，最多 500 字。 */
    @Size(max = 500)
    private String note;
}
