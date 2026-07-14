package com.xiaoyang.d_game.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoyang.d_game.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_point_log")
/**
 * 用户积分流水实体。
 *
 * <p>积分总额通过流水累加得到，sourceKey 用于保证同一业务奖励不会重复发放。</p>
 */
public class UserPointLog extends BaseEntity {

    /** 用户 ID。 */
    private Long userId;

    /** 积分来源类型，例如 CHECK_IN。 */
    private String sourceType;

    /** 幂等业务键，例如 CHECK_IN:2026-07-14。 */
    private String sourceKey;

    /** 积分变化值，正数表示增加，负数表示扣减。 */
    private Integer pointsChange;

    /** 关联目标类型，可为空。 */
    private String targetType;

    /** 关联目标 ID，可为空。 */
    private Long targetId;

    /** 流水备注。 */
    private String remark = "";
}
