package com.xiaoyang.d_game.service;

import com.xiaoyang.d_game.dto.FileResp;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务接口。
 *
 * <p>当前用于图片上传：校验文件、生成文件 key、保存到本地目录、记录上传元数据，并返回可访问 URL。</p>
 */
public interface FileService {

    /**
     * 上传文件。
     *
     * @param file 前端 multipart/form-data 上传的文件
     * @return 文件访问地址和元数据
     */
    FileResp upload(MultipartFile file);
}
