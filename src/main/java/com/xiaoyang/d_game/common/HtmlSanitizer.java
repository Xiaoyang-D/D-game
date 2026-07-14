package com.xiaoyang.d_game.common;

import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.springframework.stereotype.Component;

/**
 * 富文本清洗工具。
 *
 * <p>帖子正文允许用户提交富文本，因此必须在入库和返回前清理危险标签/属性，降低 XSS 风险。
 * 这里使用 Jsoup 的白名单模式：保留常见排版标签、链接和图片协议，剔除脚本、事件处理器和非法协议。</p>
 */
@Component
public class HtmlSanitizer {

    /**
     * 帖子内容白名单。
     *
     * <p>{@code relaxed()} 覆盖常见富文本标签；额外允许 {@code span.class} 方便前端编辑器保留样式钩子。
     * 链接和图片只允许 http/https/mailto 等安全协议，避免 {@code javascript:} 形式的注入。</p>
     */
    private static final Whitelist POST_SAFELIST = Whitelist.relaxed()
            .addTags("span")
            .addAttributes("span", "class")
            .addProtocols("a", "href", "http", "https", "mailto")
            .addProtocols("img", "src", "http", "https");

    /**
     * 清洗帖子正文 HTML。
     *
     * @param html 用户提交或历史存量中的原始 HTML，允许为空
     * @return 只包含白名单标签、属性和协议的安全 HTML
     */
    public String sanitizePostContent(String html) {
        return Jsoup.clean(html == null ? "" : html, POST_SAFELIST);
    }
}
