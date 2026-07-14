package com.xiaoyang.d_game.dto;

import lombok.Data;

@Data
/**
 * 文件上传响应。
 */
public class FileResp {

    /** 文件记录 ID。 */
    private Long id;

    /** 文件 key，通常是日期目录加文件名。 */
    private String fileKey;

    /** 文件可访问 URL。 */
    private String fileUrl;

    /** 原始文件名。 */
    private String fileName;

    /** 文件大小，单位字节。 */
    private Long fileSize;

    /** MIME 类型。 */
    private String contentType;
}
