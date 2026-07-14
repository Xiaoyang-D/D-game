package com.xiaoyang.d_game.common.enums;

import lombok.Getter;

@Getter
/**
 * 统一搜索类型枚举。
 */
public enum SearchTypeEnum {

    /** 只搜索游戏。 */
    GAME("GAME", "游戏"),

    /** 只搜索帖子。 */
    POST("POST", "帖子"),

    /** 同时搜索游戏和帖子。 */
    ALL("ALL", "全部");

    /** 前端传入的搜索类型编码。 */
    private final String code;

    /** 中文说明。 */
    private final String desc;

    SearchTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据编码解析搜索类型。
     *
     * <p>为空或非法时默认搜索全部。</p>
     */
    public static SearchTypeEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            return ALL;
        }
        for (SearchTypeEnum value : values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        return ALL;
    }
}
