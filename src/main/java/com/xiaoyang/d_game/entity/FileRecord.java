package com.xiaoyang.d_game.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoyang.d_game.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("file_record")
public class FileRecord extends BaseEntity {

    private Long userId;

    private String fileKey;

    private String fileUrl;

    private String fileName;

    private Long fileSize = 0L;

    private String contentType = "";
}
