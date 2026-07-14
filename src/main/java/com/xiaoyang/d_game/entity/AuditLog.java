package com.xiaoyang.d_game.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoyang.d_game.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("audit_log")
/**
 * 管理后台操作审计日志实体。
 *
 * <p>记录管理员对用户、帖子、评论、角色等资源的关键操作，便于问题追溯和责任定位。</p>
 */
public class AuditLog extends BaseEntity {

    /** 操作人用户 ID，通常来自当前登录管理员。 */
    private Long operatorId;

    /** 操作动作编码，例如 BAN_USER、AUDIT_POST、ASSIGN_ROLE。 */
    private String action;

    /** 被操作目标类型，例如 USER、POST、COMMENT。 */
    private String targetType = "";

    /** 被操作目标 ID；部分纯系统动作可为空。 */
    private Long targetId;

    /** 操作详情，保存后台可读的补充说明。 */
    private String detail = "";
}
