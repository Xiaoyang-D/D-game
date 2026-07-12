package com.xiaoyang.d_game.service.impl;

import com.xiaoyang.d_game.common.BizException;
import com.xiaoyang.d_game.common.ResultCode;
import com.xiaoyang.d_game.config.FileProperties;
import com.xiaoyang.d_game.dto.FileResp;
import com.xiaoyang.d_game.entity.FileRecord;
import com.xiaoyang.d_game.mapper.FileRecordMapper;
import com.xiaoyang.d_game.security.UserContext;
import com.xiaoyang.d_game.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp");

    private final FileProperties fileProperties;
    private final FileRecordMapper fileRecordMapper;

    @Override
    public FileResp upload(MultipartFile file) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.FILE_UPLOAD_ERROR, "文件为空");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new BizException(ResultCode.FILE_TYPE_NOT_ALLOWED);
        }
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String ext = extractExtension(file.getOriginalFilename());
        String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
        String fileKey = datePath + "/" + fileName;
        try {
            Path dir = Paths.get(fileProperties.getUploadDir(), datePath);
            Files.createDirectories(dir);
            Path target = dir.resolve(fileName);
            file.transferTo(target.toAbsolutePath().toFile());
        } catch (IOException e) {
            log.error("文件保存失败", e);
            throw new BizException(ResultCode.FILE_UPLOAD_ERROR);
        }
        String fileUrl = trimTrailingSlash(fileProperties.getBaseUrl()) + "/" + fileKey;

        FileRecord record = new FileRecord();
        record.setUserId(userId);
        record.setFileKey(fileKey);
        record.setFileUrl(fileUrl);
        record.setFileName(file.getOriginalFilename() == null ? fileName : file.getOriginalFilename());
        record.setFileSize(file.getSize());
        record.setContentType(contentType);
        fileRecordMapper.insert(record);

        FileResp resp = new FileResp();
        resp.setId(record.getId());
        resp.setFileKey(fileKey);
        resp.setFileUrl(fileUrl);
        resp.setFileName(record.getFileName());
        resp.setFileSize(record.getFileSize());
        resp.setContentType(contentType);
        return resp;
    }

    private String extractExtension(String originalFilename) {
        if (!StringUtils.hasText(originalFilename) || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.'));
    }

    private String trimTrailingSlash(String url) {
        if (url != null && url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }
}
