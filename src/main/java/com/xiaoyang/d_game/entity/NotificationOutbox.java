package com.xiaoyang.d_game.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoyang.d_game.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
/** 与业务共同提交的通知事件，失败重试由次数和下次执行时间控制。 */
@Data @EqualsAndHashCode(callSuper = true) @TableName("notification_outbox")
public class NotificationOutbox extends BaseEntity {
    private String eventId;
    /** 可能含通知摘要，禁止将此字段或完整实体直接写入日志。 */
    private String payload;
    /** 0 待投递，1 MQ 已确认，2 消费者已落库；仅状态 2 为最终完成。 */
    private Integer delivered = 0;
    private Integer attempts = 0;
    private LocalDateTime nextAttemptAt;
}
