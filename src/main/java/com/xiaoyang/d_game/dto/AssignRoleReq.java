package com.xiaoyang.d_game.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
/**
 * 分配角色请求。
 *
 * <p>后台管理员给指定用户绑定指定角色时使用。</p>
 */
public class AssignRoleReq {

    /** 被分配角色的用户 ID。 */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /** 要绑定的角色 ID。 */
    @NotNull(message = "角色ID不能为空")
    private Long roleId;
}
