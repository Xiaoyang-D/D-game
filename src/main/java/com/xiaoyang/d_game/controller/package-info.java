/**
 * REST 控制器包。
 *
 * <p>Controller 层只负责 HTTP 语义：路由、参数校验、登录/角色注解、统一返回包装。
 * 真正的业务判断、事务提交、数据库写入和通知发送都下沉到 Service 层，便于复用和测试。</p>
 */
package com.xiaoyang.d_game.controller;
