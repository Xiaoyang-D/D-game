package com.xiaoyang.d_game.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoyang.d_game.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("file_record")
/**
 * 文件上传记录实体。
 *
 * <p>保存上传文件的归属、访问地址和元数据，文件内容本身保存在本地磁盘或未来的对象存储中。</p>
 */
public class FileRecord extends BaseEntity {

    /** 上传用户 ID。 */
    private Long userId;

    /** 文件业务 key，通常是日期目录加 UUID 文件名。 */
    private String fileKey;

    /** 文件对外访问 URL。 */
    private String fileUrl;

    /** 用户上传时的原始文件名。 */
    private String fileName;

    /** 文件大小，单位字节。 */
    private Long fileSize = 0L;

    /** MIME 类型，例如 image/png。 */
    private String contentType = "";
}
