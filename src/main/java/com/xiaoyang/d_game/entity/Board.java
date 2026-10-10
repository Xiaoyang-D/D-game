package com.xiaoyang.d_game.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoyang.d_game.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("board")
/**
 * 社区版块实体。
 *
 * <p>帖子必须归属到某个版块，版块用于前台导航、筛选和后台内容组织。</p>
 */
public class Board extends BaseEntity {

    /** 版块名称，例如“综合讨论”“攻略分享”。 */
    private String name;

    /** 版块说明，用于前端展示该版块的讨论范围。 */
    private String description = "";

    /** 排序值，越小越靠前。 */
    private Integer sortOrder = 0;
    /** 私有分区只能在关联的游戏中使用。 */
    private Boolean scopePrivate = false;
    /** 未指定游戏时使用的发布权限。 */
    private String publishPolicy = "LOGIN";
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String iconKey = "forum";
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String iconUrl = "";
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String bannerUrl = "";
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private Boolean enabled = true;
}
