package com.xiaoyang.d_game.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "file")
/**
 * 文件上传相关配置。
 *
 * <p>通过 {@code file.*} 配置绑定，通常由 profile 配置或环境变量提供。
 * 上传服务只保存本地文件路径和可访问 URL；如果后续切换到对象存储，可以优先从这里扩展配置项。</p>
 */
public class FileProperties {

    /**
     * 文件落盘目录。
     *
     * <p>本地开发一般指向项目下的 {@code uploads} 目录；容器/生产环境应挂载到持久化卷，避免容器重建后文件丢失。</p>
     */
    private String uploadDir;

    /**
     * 文件对外访问基础地址。
     *
     * <p>上传成功后会和文件 key 拼成完整 URL 返回给前端，例如头像、帖子图片、游戏封面都会依赖该地址展示。</p>
     */
    private String baseUrl;
}
