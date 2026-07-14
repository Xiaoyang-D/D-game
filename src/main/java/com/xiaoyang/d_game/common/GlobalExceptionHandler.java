package com.xiaoyang.d_game.common;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
/**
 * 全局异常处理器。
 *
 * <p>所有 Controller 抛出的异常都会在这里统一转换为接口响应，保证前端始终拿到稳定的 JSON 结构。
 * 业务异常返回业务错误码；参数异常返回明确的字段校验提示；未预期异常记录完整日志并返回通用系统错误。</p>
 */
public class GlobalExceptionHandler {

    /**
     * 处理业务异常。
     *
     * <p>业务异常代表可预期失败，所以只记录 warn 日志，并把异常里的业务码和提示直接返回给前端。</p>
     */
    @ExceptionHandler(BizException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleBizException(BizException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数校验异常。
     *
     * <p>兼容三类常见校验来源：{@code @RequestBody}、表单/查询参数绑定、以及方法参数约束。
     * 优先返回第一个字段错误，减少前端排查成本。</p>
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, ConstraintViolationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidationException(Exception e) {
        String message = "请求参数错误";
        if (e instanceof MethodArgumentNotValidException ex && ex.getBindingResult().getFieldError() != null) {
            message = ex.getBindingResult().getFieldError().getDefaultMessage();
        } else if (e instanceof BindException ex && ex.getBindingResult().getFieldError() != null) {
            message = ex.getBindingResult().getFieldError().getDefaultMessage();
        } else if (e instanceof ConstraintViolationException ex) {
            message = ex.getMessage();
        }
        return Result.fail(ResultCode.BAD_REQUEST.getCode(), message);
    }

    /**
     * 兜底处理未预期异常。
     *
     * <p>这里会打印完整堆栈，便于后端定位问题；对外只暴露“系统异常”，避免泄露数据库结构、路径或密钥信息。</p>
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail(ResultCode.INTERNAL_ERROR);
    }
}
