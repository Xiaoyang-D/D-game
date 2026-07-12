package com.xiaoyang.d_game.common;

import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.springframework.stereotype.Component;

/** Sanitizes user-authored rich text before persistence and presentation. */
@Component
public class HtmlSanitizer {

    private static final Whitelist POST_SAFELIST = Whitelist.relaxed()
            .addTags("span")
            .addAttributes("span", "class")
            .addProtocols("a", "href", "http", "https", "mailto")
            .addProtocols("img", "src", "http", "https");

    public String sanitizePostContent(String html) {
        return Jsoup.clean(html == null ? "" : html, POST_SAFELIST);
    }
}
