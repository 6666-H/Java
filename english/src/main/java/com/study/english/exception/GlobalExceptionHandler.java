package com.study.english.exception;

import com.study.english.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常：BusinessException 返回 200 + code/msg；租户 403 由拦截器直接写响应。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException ex, HttpServletRequest request) {
        log.warn("BusinessException: {} path={}", ex.getMessage(), request.getRequestURI());
        return ResponseEntity.ok(Result.fail(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleOther(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception path=" + request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail("INTERNAL_ERROR", "服务器内部错误"));
    }
}
