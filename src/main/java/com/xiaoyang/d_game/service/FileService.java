package com.xiaoyang.d_game.service;

import com.xiaoyang.d_game.dto.FileResp;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {

    FileResp upload(MultipartFile file);
}
