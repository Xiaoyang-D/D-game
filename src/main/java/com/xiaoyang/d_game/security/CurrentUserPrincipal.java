package com.xiaoyang.d_game.security;

/** 写入 Spring Security 上下文的应用用户主体，只保存业务所需的基本身份信息。 */
public record CurrentUserPrincipal(Long userId, String username) {
}
