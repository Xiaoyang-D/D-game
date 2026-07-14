package com.xiaoyang.d_game.controller;

import com.xiaoyang.d_game.common.Result;
import com.xiaoyang.d_game.dto.FileResp;
import com.xiaoyang.d_game.security.RequireLogin;
import com.xiaoyang.d_game.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传接口。
 *
 * <p>当前主要用于图片上传。上传需要登录，服务层会校验类型、保存文件、写入文件记录，并返回可访问 URL。</p>
 */
@Tag(name = "文件")
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /**
     * 上传单个图片文件。
     *
     * <p>表单字段名必须为 {@code file}，前端应使用 multipart/form-data。</p>
     */
    @RequireLogin
    @Operation(summary = "上传图片")
    @PostMapping("/upload")
    public Result<FileResp> upload(@RequestParam("file") MultipartFile file) {
        return Result.success(fileService.upload(file));
    }
}
