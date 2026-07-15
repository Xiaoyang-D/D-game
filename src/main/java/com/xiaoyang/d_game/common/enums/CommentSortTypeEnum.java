package com.xiaoyang.d_game.common.enums;

import com.xiaoyang.d_game.common.BizException;
import com.xiaoyang.d_game.common.ResultCode;

import java.util.Locale;

/** Sort modes supported by the public comment list. */
public enum CommentSortTypeEnum {

    DEFAULT,
    EARLIEST,
    LATEST;

    public static CommentSortTypeEnum from(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BizException(ResultCode.BAD_REQUEST, "评论排序参数无效");
        }
    }
}
