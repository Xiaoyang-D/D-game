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
/**
 * 本地文件上传服务实现。
 *
 * <p>当前保存到本地磁盘，并把文件元数据写入 {@code file_record} 表。
 * 目录按日期拆分，文件名使用 UUID，避免原始文件名冲突和路径猜测。</p>
 */
public class FileServiceImpl implements FileService {

    /**
     * 允许上传的图片 MIME 类型。
     *
     * <p>这里只检查浏览器/客户端传来的 contentType；生产环境更严格时可以增加文件头魔数校验。</p>
     */
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp");

    private final FileProperties fileProperties;
    private final FileRecordMapper fileRecordMapper;

    @Override
    /**
     * 上传文件。
     */
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
        // 使用 yyyy/MM/dd 分目录，避免单目录文件过多影响文件系统性能。
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String ext = extractExtension(file.getOriginalFilename());
        // UUID 文件名避免用户上传同名文件互相覆盖。
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
        // 对外 URL 只暴露 fileKey，不暴露服务器真实磁盘路径。
        String fileUrl = trimTrailingSlash(fileProperties.getBaseUrl()) + "/" + fileKey;

        // 保存上传记录，便于后续做文件归属、清理、审计或资源管理。
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

    /**
     * 从原始文件名中提取扩展名。
     *
     * <p>扩展名只用于保留文件后缀和浏览器识别，安全判断仍以 MIME 类型为准。</p>
     */
    private String extractExtension(String originalFilename) {
        if (!StringUtils.hasText(originalFilename) || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.'));
    }

    /**
     * 去掉 baseUrl 尾部斜杠，避免拼接 URL 时出现双斜杠。
     */
    private String trimTrailingSlash(String url) {
        if (url != null && url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }
}
