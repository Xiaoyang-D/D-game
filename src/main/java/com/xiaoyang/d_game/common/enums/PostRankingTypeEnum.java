package com.xiaoyang.d_game.common.enums;

import lombok.Getter;

@Getter
/**
 * 帖子排行榜类型枚举。
 */
public enum PostRankingTypeEnum {

    /** 热度榜：按浏览、点赞、评论、收藏加权排序。 */
    HOT("hot", "热度榜"),

    /** 最新榜：按创建时间倒序排序。 */
    LATEST("latest", "最新榜");

    /** 前端传入的类型编码。 */
    private final String code;

    /** 中文说明。 */
    private final String desc;

    PostRankingTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据前端编码解析帖子排行榜类型。
     *
     * <p>为空或非法时默认热度榜。</p>
     */
    public static PostRankingTypeEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            return HOT;
        }
        for (PostRankingTypeEnum value : values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        return HOT;
    }
}
