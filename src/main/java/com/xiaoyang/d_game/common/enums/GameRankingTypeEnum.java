package com.xiaoyang.d_game.common.enums;

import lombok.Getter;

@Getter
/**
 * 游戏排行榜类型枚举。
 */
public enum GameRankingTypeEnum {

    /** 评分榜：优先按平均分排序。 */
    RATING("rating", "评分榜"),

    /** 人气榜：优先按评分人数排序。 */
    POPULAR("popular", "人气榜");

    /** 前端传入的类型编码。 */
    private final String code;

    /** 中文说明。 */
    private final String desc;

    GameRankingTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据前端编码解析排行榜类型。
     *
     * <p>为空或非法时默认评分榜，保证接口有稳定回退。</p>
     */
    public static GameRankingTypeEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            return RATING;
        }
        for (GameRankingTypeEnum value : values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        return RATING;
    }
}
