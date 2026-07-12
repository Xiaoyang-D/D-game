package com.xiaoyang.d_game.dto;

import lombok.Data;

@Data
public class FileResp {

    private Long id;

    private String fileKey;

    private String fileUrl;

    private String fileName;

    private Long fileSize;

    private String contentType;
}
