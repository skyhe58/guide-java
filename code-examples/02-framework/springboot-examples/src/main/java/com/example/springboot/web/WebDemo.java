package com.example.springboot.web;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Web 开发演示
 *
 * <p>本类演示以下知识点：</p>
 * <ul>
 *   <li>RESTful Controller</li>
 *   <li>全局异常处理（@RestControllerAdvice）</li>
 *   <li>拦截器（HandlerInterceptor）</li>
 *   <li>过滤器（Filter）</li>
 *   <li>参数校验（@Valid）</li>
 * </ul>
 */
public class WebDemo {

    // ==================== 1. 统一返回结果 ====================

    /**
     * 统一 API 返回格式
     */
    public record Result<T>(int code, String message, T data) {

        public static <T> Result<T> ok(T data) {
            return new Result<>(200, "success", data);
        }

        public static <T> Result<T> fail(int code, String message) {
            return new Result<>(code, message, null);
        }
    }

    // ==================== 2. 请求参数校验 DTO ====================

    /**
     * 用户创建请求 — 演示参数校验注解
     */
    public record UserCreateRequest(
            @NotBlank(message = "用户名不能为空")
            String username,

            @Email(message = "邮箱格式不正确")
            @NotBlank(message = "邮箱不能为空")
            String email,

            @Min(value = 1, message = "年龄最小为1")
            @Max(value = 150, message = "年龄最大为150")
            Integer age
    ) {
    }

    /**
     * 用户响应 DTO
     */
    public record UserResponse(Long id, String username, String email, Integer age) {
    }

    // ==================== 3. RESTful Controller ====================

    /**
     * 用户 RESTful API 控制器
     */
    @RestController
    @RequestMapping("/api/users")
    public static class UserController {

        private static final Logger log = LoggerFactory.getLogger(UserController.class);

        @GetMapping("/{id}")
        public Result<UserResponse> getUser(@PathVariable Long id) {
            log.info("查询用户: {}", id);
            var user = new UserResponse(id, "user-" + id, "user" + id + "@example.com", 25);
            return Result.ok(user);
        }

        @PostMapping
        public Result<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
            log.info("创建用户: {}", request);
            var user = new UserResponse(1L, request.username(), request.email(), request.age());
            return Result.ok(user);
        }

        @GetMapping("/hello")
        public Result<String> hello(@RequestParam(defaultValue = "World") String name) {
            return Result.ok("Hello, " + name + "!");
        }
    }

    // ==================== 4. 全局异常处理 ====================

    /**
     * 自定义业务异常
     */
    public static class BusinessException extends RuntimeException {
        private final int code;

        public BusinessException(int code, String message) {
            super(message);
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }

    /**
     * 全局异常处理器
     *
     * <p>@RestControllerAdvice = @ControllerAdvice + @ResponseBody</p>
     * <p>按异常类型分别处理，返回统一格式</p>
     */
    @RestControllerAdvice
    public static class GlobalExceptionHandler {

        private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

        // 参数校验异常
        @ExceptionHandler(MethodArgumentNotValidException.class)
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        public Result<?> handleValidation(MethodArgumentNotValidException e) {
            String message = e.getBindingResult().getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .collect(Collectors.joining(", "));
            log.warn("参数校验失败: {}", message);
            return Result.fail(400, message);
        }

        // 业务异常
        @ExceptionHandler(BusinessException.class)
        public Result<?> handleBusiness(BusinessException e) {
            log.warn("业务异常: {}", e.getMessage());
            return Result.fail(e.getCode(), e.getMessage());
        }

        // 兜底异常
        @ExceptionHandler(Exception.class)
        @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
        public Result<?> handleException(Exception e) {
            log.error("系统异常", e);
            return Result.fail(500, "服务器内部错误");
        }
    }

    // ==================== 5. 拦截器 ====================

    /**
     * 请求日志拦截器
     *
     * <p>拦截器在 DispatcherServlet 之后、Controller 之前执行。
     * 可以方便地获取 Spring Bean。</p>
     */
    @Component
    public static class RequestLogInterceptor implements HandlerInterceptor {

        private static final Logger log = LoggerFactory.getLogger(RequestLogInterceptor.class);

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                                 Object handler) {
            request.setAttribute("startTime", System.currentTimeMillis());
            log.info("[Interceptor] {} {} 开始处理", request.getMethod(), request.getRequestURI());
            return true; // true 放行，false 拦截
        }

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                    Object handler, Exception ex) {
            long startTime = (long) request.getAttribute("startTime");
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("[Interceptor] {} {} 处理完成, 耗时: {}ms, 状态: {}",
                    request.getMethod(), request.getRequestURI(), elapsed, response.getStatus());
        }
    }

    // ==================== 6. 过滤器 ====================

    /**
     * 请求编码过滤器
     *
     * <p>过滤器是 Servlet 规范，在 DispatcherServlet 之前执行，
     * 作用于所有请求（包括静态资源）。</p>
     */
    public static class EncodingFilter implements Filter {

        private static final Logger log = LoggerFactory.getLogger(EncodingFilter.class);

        @Override
        public void doFilter(ServletRequest request, ServletResponse response,
                             FilterChain chain) throws IOException, ServletException {
            request.setCharacterEncoding("UTF-8");
            response.setCharacterEncoding("UTF-8");
            log.debug("[Filter] 设置编码 UTF-8");
            chain.doFilter(request, response);
        }
    }

    // ==================== 7. Web 配置 ====================

    /**
     * Web MVC 配置 — 注册拦截器和过滤器
     */
    @Configuration
    public static class WebConfig implements WebMvcConfigurer {

        private final RequestLogInterceptor requestLogInterceptor;

        public WebConfig(RequestLogInterceptor requestLogInterceptor) {
            this.requestLogInterceptor = requestLogInterceptor;
        }

        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            registry.addInterceptor(requestLogInterceptor)
                    .addPathPatterns("/api/**");
        }

        @Bean
        public FilterRegistrationBean<EncodingFilter> encodingFilter() {
            var registration = new FilterRegistrationBean<>(new EncodingFilter());
            registration.addUrlPatterns("/*");
            registration.setOrder(1);
            return registration;
        }
    }
}
