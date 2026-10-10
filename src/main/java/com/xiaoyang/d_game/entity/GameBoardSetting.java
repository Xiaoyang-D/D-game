package com.xiaoyang.d_game.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoyang.d_game.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 保留原分区 ID，为不同游戏维护独立展示及发布设置。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("game_board_setting")
public class GameBoardSetting extends BaseEntity {
    private Long gameId;
    private Long boardId;
    private String name;
    private String description = "";
    private String iconKey = "forum";
    private String iconUrl = "";
    private String bannerUrl = "";
    private Integer sortOrder = 0;
    private Boolean enabled = true;
    private String publishPolicy = "LOGIN";
}
