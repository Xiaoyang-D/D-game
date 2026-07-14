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
}
