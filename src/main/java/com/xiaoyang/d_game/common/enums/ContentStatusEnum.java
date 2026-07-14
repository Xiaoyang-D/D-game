package com.xiaoyang.d_game.common.enums;

import lombok.Getter;

@Getter
/**
 * 内容审核状态枚举。
 *
 * <p>帖子和评论共用该状态机：草稿、待审核、已通过、已拒绝。</p>
 */
public enum ContentStatusEnum {

    /** 草稿，仅作者自己可见。 */
    DRAFT(0, "草稿"),

    /** 待管理员审核。 */
    PENDING(1, "待审核"),

    /** 审核通过，可公开展示。 */
    APPROVED(2, "已通过"),

    /** 审核拒绝，不进入公开列表。 */
    REJECTED(3, "已拒绝");

    /** 数据库存储的状态码。 */
    private final int code;

    /** 中文说明。 */
    private final String desc;

    /**
     * 枚举构造器。
     */
    ContentStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
