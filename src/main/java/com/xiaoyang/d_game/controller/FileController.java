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

@Tag(name = "文件")
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @RequireLogin
    @Operation(summary = "上传图片")
    @PostMapping("/upload")
    public Result<FileResp> upload(@RequestParam("file") MultipartFile file) {
        return Result.success(fileService.upload(file));
    }
}
