package com.xiaoyang.d_game.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.xiaoyang.d_game.common.BaseEntity;
import com.xiaoyang.d_game.common.enums.ContentStatusEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("post")
public class Post extends BaseEntity {

    private Long boardId;

    private Long gameId;

    private Long userId;

    private String title;

    private String content;

    private Integer status = ContentStatusEnum.PENDING.getCode();

    private Integer viewCount = 0;

    private Integer likeCount = 0;

    private Integer commentCount = 0;

    private Integer favoriteCount = 0;

    @Version
    private Integer version = 0;
}
