package com.xiaoyang.d_game.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 为 HTTP 请求生成服务端排查编号，在响应头与同线程异常日志之间建立关联。
 * 只记录方法、路径、状态和耗时，不读取请求正文、查询参数、Cookie 或认证头。
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {
    public static final String REQUEST_ID = "requestId";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 不信任客户端传入的编号，避免日志注入；退出请求时恢复原上下文，防止线程复用串号。
        String previous = MDC.get(REQUEST_ID);
        String requestId = UUID.randomUUID().toString();
        long started = System.nanoTime();
        MDC.put(REQUEST_ID, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        boolean completed = false;
        try {
            filterChain.doFilter(request, response);
            completed = true;
        } finally {
            try {
                // 完成标志区分正常返回与异常中断，状态码可能尚未被外层容器更新。
                log.debug("接口请求结束，requestId={}，method={}，uri={}，status={}，完成={}，elapsedMs={}",
                        requestId, request.getMethod(), request.getRequestURI(), response.getStatus(), completed,
                        TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
            } finally {
                if (previous == null) {
                    MDC.remove(REQUEST_ID);
                } else {
                    MDC.put(REQUEST_ID, previous);
                }
            }
        }
    }
}
