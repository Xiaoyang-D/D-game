package com.xiaoyang.d_game.dto;

import lombok.Data;

@Data
/**
 * 甯栧瓙鍒嗛〉鏌ヨ璇锋眰銆?
 */
public class PostQueryReq {

    /** 鐗堝潡 ID 绛涢€夈€?*/
    private Long boardId;

    /** 浣滆€?ID 绛涢€夈€?*/
    private Long authorId;

    /** 鍏宠仈娓告垙 ID 绛涢€夈€?*/
    private Long gameId;

    /** 鏍囬鍏抽敭瀛椼€?*/
    private String keyword;

    /** 椤电爜锛屼粠 1 寮€濮嬨€?*/
    private Long page = 1L;

    /** 姣忛〉鏉℃暟銆?*/
    private Long size = 10L;
}
