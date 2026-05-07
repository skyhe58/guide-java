package com.example.springcloud.boot.handler;

import com.example.springcloud.boot.exception.BusinessException;
import com.example.springcloud.common.Result;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 全局异常处理器 — @ControllerAdvice + @ExceptionHandler
 *
 * <p>统一捕获 Controller 层抛出的异常，转换为标准的 {@link Result} 响应格式。
 * <ul>
 *   <li>BusinessException → 返回业务错误码和消息</li>
 *   <li>MethodArgumentNotValidException → 返回参数校验错误详情</li>
 *   <li>ConstraintViolationException → 返回约束违反详情</li>
 *   <li>RuntimeException → 返回 500 内部错误</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理自定义业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Map<String, Object>> handleBusinessException(BusinessException e) {
        log.warn("[业务异常] code={}, message={}", e.getCode(), e.getMessage());
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("异常类型", e.getClass().getSimpleName());
        detail.put("错误消息", e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理 @Valid 参数校验异常（@RequestBody 校验失败）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Map<String, Object>> handleValidationException(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "校验失败",
                        (v1, v2) -> v1,
                        LinkedHashMap::new
                ));

        log.warn("[参数校验失败] {}", fieldErrors);

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("异常类型", "MethodArgumentNotValidException");
        detail.put("字段错误", fieldErrors);
        return Result.fail(400, "参数校验失败");
    }

    /**
     * 处理 @Validated 约束违反异常（@RequestParam / @PathVariable 校验失败）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Map<String, Object>> handleConstraintViolation(ConstraintViolationException e) {
        Map<String, String> violations = e.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        v -> v.getPropertyPath().toString(),
                        v -> v.getMessage(),
                        (v1, v2) -> v1,
                        LinkedHashMap::new
                ));

        log.warn("[约束违反] {}", violations);

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("异常类型", "ConstraintViolationException");
        detail.put("违反约束", violations);
        return Result.fail(400, "参数约束违反");
    }

    /**
     * 处理其他运行时异常（兜底）
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Map<String, String>> handleRuntimeException(RuntimeException e) {
        log.error("[运行时异常] {}", e.getMessage(), e);
        Map<String, String> detail = new LinkedHashMap<>();
        detail.put("异常类型", e.getClass().getSimpleName());
        detail.put("错误消息", e.getMessage());
        return Result.fail(500, "服务器内部错误：" + e.getMessage());
    }
}
