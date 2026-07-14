package com.xiaoyang.d_game.common;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
/**
 * 所有数据库实体的基础字段。
 *
 * <p>业务表普遍包含主键、创建时间、修改时间和逻辑删除字段，放在基类里可以避免每个 Entity 重复声明。
 * MyBatis-Plus 会根据这里的注解自动生成雪花 ID、填充时间字段，并在删除时改写 {@code is_deleted}。</p>
 */
public abstract class BaseEntity implements Serializable {

    /**
     * 全局唯一主键。
     *
     * <p>使用 MyBatis-Plus 的 {@link IdType#ASSIGN_ID} 生成雪花 ID。
     * 雪花 ID 可能超过 JavaScript 安全整数范围，所以序列化给前端时统一转为字符串。</p>
     */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 创建时间。
     *
     * <p>插入数据时由 {@code MetaObjectHandler} 自动填充，业务代码通常不需要手动赋值。</p>
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime gmtCreate;

    /**
     * 最近修改时间。
     *
     * <p>插入和更新时自动填充，用于列表排序、审计追踪和前端展示。</p>
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime gmtModified;

    /**
     * 逻辑删除标记：0 表示未删除，1 表示已删除。
     *
     * <p>{@link TableLogic} 会让 MyBatis-Plus 在普通查询中自动过滤已删除记录，
     * 删除操作也会变成更新该字段，避免物理删除破坏历史关联。</p>
     */
    @TableLogic
    private Integer isDeleted = 0;
}
