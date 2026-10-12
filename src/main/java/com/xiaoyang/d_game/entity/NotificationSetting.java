package com.xiaoyang.d_game.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoyang.d_game.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
/** 四类通知偏好默认开启，系统、审核和私信不受这些开关影响。 */
@Data @EqualsAndHashCode(callSuper = true) @TableName("notification_setting")
public class NotificationSetting extends BaseEntity {
    private Long userId;
    private Integer replies = 1;
    private Integer mentions = 1;
    private Integer likes = 1;
    private Integer social = 1;
}
