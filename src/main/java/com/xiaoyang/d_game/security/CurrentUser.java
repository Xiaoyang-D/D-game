package com.xiaoyang.d_game.security;

import com.xiaoyang.d_game.common.BizException;
import com.xiaoyang.d_game.common.ResultCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** 从 Spring Security 的 SecurityContext 获取当前用户，不再自行维护请求级 ThreadLocal。 */
public final class CurrentUser {

    private CurrentUser() {
    }

    /** 获取当前登录用户 ID；未登录时抛出未授权异常。 */
    public static Long getUserId() {
        // 必须登录的业务直接复用统一主体校验，避免各处重复判断 null。
        return getPrincipal().userId();
    }

    /** 获取当前用户 ID；公开接口需要兼容匿名访问时返回 null。 */
    public static Long getOptionalUserId() {
        // 公开接口可能没有认证对象，因此这里不能调用 getPrincipal() 直接抛异常。
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // 只有真实的 CurrentUserPrincipal 才代表 JWT 认证成功，匿名主体不算登录。
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CurrentUserPrincipal principal)) {
            return null;
        }
        return principal.userId();
    }

    /** 获取当前认证主体；用于必须登录的业务逻辑。 */
    public static CurrentUserPrincipal getPrincipal() {
        // SecurityContext 由 Spring Security 在请求开始时创建，并在请求结束时清理。
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // 同时检查认证状态和主体类型，防止把匿名主体或其他认证方式误当成业务用户。
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CurrentUserPrincipal principal)) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return principal;
    }

    /** 判断当前认证主体是否拥有指定角色，调用方可传 ADMIN 或 ROLE_ADMIN。 */
    public static boolean hasRole(String role) {
        // Spring Security 的角色约定带 ROLE_ 前缀，兼容调用方传入两种形式。
        String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // 遍历当前认证对象的权限集合，返回是否精确匹配目标角色。
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> authority.equals(grantedAuthority.getAuthority()));
    }
}
