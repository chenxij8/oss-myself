package com.oss.file.app.exception;

import com.oss.file.common.exception.BusinessException;
import com.oss.file.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 * 捕获和处理应用中的各种异常
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * 业务异常处理
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException ex, WebRequest request) {
        log.warn("捕获业务异常: {}", ex.getMessage());
        ApiResponse<Object> response = new ApiResponse<>(ex.getCode(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * 重写父类的参数验证异常处理
     * 注意：这里不再使用 @ExceptionHandler 注解，而是使用 @Override
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        log.warn("捕获参数验证异常");

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ApiResponse<Map<String, String>> response = new ApiResponse<>(400, "参数验证失败", errors);

        // 注意：返回值类型必须与父类定义的 ResponseEntity<Object> 匹配
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * 权限异常处理
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {

        log.warn("捕获权限异常: {}", ex.getMessage());
        ApiResponse<Object> response = ApiResponse.forbidden();
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(response);
    }

    /**
     * 通用异常处理
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGlobalException(Exception ex, WebRequest request) {
        log.error("捕获未处理的异常", ex);

        ApiResponse<Object> response = new ApiResponse<>(500, "系统内部错误: " + ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    /**
     * 重新定义父类其他内部异常的返回格式（可选）
     * 避免父类默认返回空的 Body
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {

        if (body == null) {
            body = new ApiResponse<>(statusCode.value(), ex.getMessage());
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }
}