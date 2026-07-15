package com.xiaoyang.d_game.common.enums;

import com.xiaoyang.d_game.common.BizException;
import com.xiaoyang.d_game.common.ResultCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CommentSortTypeEnumTest {

    @Test
    void parsesSupportedSortModesCaseInsensitively() {
        assertEquals(CommentSortTypeEnum.DEFAULT, CommentSortTypeEnum.from(null));
        assertEquals(CommentSortTypeEnum.DEFAULT, CommentSortTypeEnum.from("default"));
        assertEquals(CommentSortTypeEnum.EARLIEST, CommentSortTypeEnum.from("EARLIEST"));
        assertEquals(CommentSortTypeEnum.LATEST, CommentSortTypeEnum.from("latest"));
    }

    @Test
    void rejectsUnsupportedSortModes() {
        BizException exception = assertThrows(BizException.class, () -> CommentSortTypeEnum.from("popular"));

        assertEquals(ResultCode.BAD_REQUEST.getCode(), exception.getCode());
    }
}
