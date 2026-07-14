package com.xiaoyang.d_game.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoyang.d_game.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_badge")
/**
 * 用户徽章实体。
 *
 * <p>记录用户已经获得的成长徽章。</p>
 */
public class UserBadge extends BaseEntity {

    /** 用户 ID。 */
    private Long userId;

    /** 徽章编码，取值见 BadgeCodeEnum。 */
    private String badgeCode;

    /** 徽章名称，冗余保存便于历史展示。 */
    private String badgeName;

    /** 徽章描述。 */
    private String description = "";
}
