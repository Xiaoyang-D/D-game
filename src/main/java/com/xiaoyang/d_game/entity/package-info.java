/**
 * 数据库实体包。
 *
 * <p>Entity 与数据库表字段一一对应，并继承 {@code BaseEntity} 复用主键、创建时间、修改时间和逻辑删除字段。
 * 面向接口返回时不要直接暴露 Entity，避免把密码哈希、逻辑删除位或内部审计字段带给前端。</p>
 */
package com.xiaoyang.d_game.entity;
