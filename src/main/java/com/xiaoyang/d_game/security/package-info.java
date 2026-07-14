/**
 * 鉴权与登录上下文包。
 *
 * <p>本项目使用 JWT 做无状态认证，通过拦截器解析令牌、校验登录和角色，并把当前用户信息放入 ThreadLocal。
 * 业务层需要当前用户时应读取 {@code UserContext}，不要从 Controller 手动向下传递用户 ID。</p>
 */
package com.xiaoyang.d_game.security;
