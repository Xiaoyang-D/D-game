/**
 * D_game 后端主包。
 *
 * <p>项目采用典型的 Spring Boot 分层结构：Controller 负责接收 HTTP 请求并做轻量参数绑定，
 * Service 负责业务规则、事务和跨表协作，Mapper 负责数据库访问，Entity/DTO 分别承载持久化模型和接口模型。
 * 新增后端功能时，优先沿用这个分层，避免把鉴权、事务或多表状态维护逻辑散落到 Controller 中。</p>
 */
package com.xiaoyang.d_game;
