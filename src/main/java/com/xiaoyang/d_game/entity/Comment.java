package com.xiaoyang.d_game.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoyang.d_game.common.BaseEntity;
import com.xiaoyang.d_game.common.enums.ContentStatusEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("comment")
public class Comment extends BaseEntity {

    private Long postId;

    private Long userId;

    private Long parentId = 0L;

    private String content;

    private Integer likeCount = 0;

    private Integer status = ContentStatusEnum.APPROVED.getCode();
}
