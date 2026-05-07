package com.example.springcloud.feign;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Feign Fallback 工厂 — 服务降级处理
 *
 * <p>使用 FallbackFactory 而非 Fallback 的优势：
 * <ul>
 *   <li>可以获取到导致降级的异常信息（Throwable cause）</li>
 *   <li>方便记录异常日志，便于排查问题</li>
 *   <li>可以根据不同异常类型返回不同的降级数据</li>
 * </ul>
 */
@Component
public class UserFeignFallbackFactory implements FallbackFactory<UserFeignClient> {

    private static final Logger log = LoggerFactory.getLogger(UserFeignFallbackFactory.class);

    @Override
    public UserFeignClient create(Throwable cause) {
        log.error("[Feign Fallback] user-service 调用失败，触发降级: {}", cause.getMessage());

        return new UserFeignClient() {
            @Override
            public UserDTO getUser(Long id) {
                log.warn("[Fallback] getUser({}) 降级，原因: {}", id, cause.getMessage());
                return new UserDTO(id, "降级用户", "fallback@example.com", "N/A");
            }

            @Override
            public UserDTO createUser(UserDTO user) {
                log.warn("[Fallback] createUser 降级，原因: {}", cause.getMessage());
                UserDTO fallback = new UserDTO(0L, user.getName(), user.getEmail(), user.getPhone());
                return fallback;
            }

            @Override
            public List<UserDTO> listUsers() {
                log.warn("[Fallback] listUsers 降级，原因: {}", cause.getMessage());
                return Collections.singletonList(
                        new UserDTO(0L, "降级用户", "fallback@example.com", "N/A")
                );
            }
        };
    }
}
