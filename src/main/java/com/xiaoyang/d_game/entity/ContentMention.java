package com.xiaoyang.d_game.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoyang.d_game.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
/** 按用户 ID 识别提及，保留首次通知时间，避免移除后重新添加造成重复通知。 */
@Data @EqualsAndHashCode(callSuper = true) @TableName("content_mention")
public class ContentMention extends BaseEntity {
    /** 来源类型：1 帖子，2 评论。 */
    private Integer sourceType;
    private Long sourceId;
    private Long userId;
    /** 评论的 UTF-16 起止范围；帖子从富文本标签识别，无文本范围。 */
    private String rangesJson = "[]";
    private LocalDateTime notifiedAt;
}
