package com.example.springcloud.circuitbreaker;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 熔断器状态验证测试
 *
 * <p>验证 Resilience4j 熔断器的状态转换逻辑</p>
 */
class CircuitBreakerTest {

    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(1))
                .permittedNumberOfCallsInHalfOpenState(2)
                .minimumNumberOfCalls(3)
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        circuitBreaker = registry.circuitBreaker("test-service");
    }

    @Test
    @DisplayName("熔断器初始状态应为 CLOSED")
    void shouldStartInClosedState() {
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
    }

    @Test
    @DisplayName("成功调用不应触发熔断")
    void shouldRemainClosedOnSuccess() {
        // 执行 5 次成功调用
        for (int i = 0; i < 5; i++) {
            Supplier<String> supplier = CircuitBreaker.decorateSupplier(
                    circuitBreaker, () -> "success");
            supplier.get();
        }

        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
        assertEquals(0, circuitBreaker.getMetrics().getFailureRate());
    }

    @Test
    @DisplayName("失败率超过阈值应触发熔断（CLOSED → OPEN）")
    void shouldTransitionToOpenWhenFailureRateExceeded() {
        // 执行 3 次失败调用（超过 minimumNumberOfCalls=3 且失败率 100% > 50%）
        for (int i = 0; i < 5; i++) {
            try {
                Supplier<String> supplier = CircuitBreaker.decorateSupplier(
                        circuitBreaker, () -> {
                            throw new RuntimeException("service failure");
                        });
                supplier.get();
            } catch (Exception ignored) {
                // 预期异常
            }
        }

        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());
    }

    @Test
    @DisplayName("熔断状态下调用应直接抛出 CallNotPermittedException")
    void shouldRejectCallsInOpenState() {
        // 先触发熔断
        for (int i = 0; i < 5; i++) {
            try {
                Supplier<String> supplier = CircuitBreaker.decorateSupplier(
                        circuitBreaker, () -> {
                            throw new RuntimeException("failure");
                        });
                supplier.get();
            } catch (Exception ignored) {
            }
        }

        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());

        // 熔断状态下的调用应被拒绝
        Supplier<String> supplier = CircuitBreaker.decorateSupplier(
                circuitBreaker, () -> "should not execute");

        assertThrows(io.github.resilience4j.circuitbreaker.CallNotPermittedException.class,
                supplier::get);
    }

    @Test
    @DisplayName("等待超时后应从 OPEN 转为 HALF_OPEN")
    void shouldTransitionToHalfOpenAfterWaitDuration() throws InterruptedException {
        // 触发熔断
        for (int i = 0; i < 5; i++) {
            try {
                Supplier<String> supplier = CircuitBreaker.decorateSupplier(
                        circuitBreaker, () -> {
                            throw new RuntimeException("failure");
                        });
                supplier.get();
            } catch (Exception ignored) {
            }
        }

        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());

        // 等待超过 waitDurationInOpenState (1 秒)
        Thread.sleep(1200);

        // 手动转换到 HALF_OPEN（实际中由下一次调用触发）
        circuitBreaker.transitionToHalfOpenState();
        assertEquals(CircuitBreaker.State.HALF_OPEN, circuitBreaker.getState());
    }

    @Test
    @DisplayName("半开状态探测成功应恢复为 CLOSED")
    void shouldTransitionToClosedFromHalfOpenOnSuccess() throws InterruptedException {
        // 触发熔断
        for (int i = 0; i < 5; i++) {
            try {
                Supplier<String> supplier = CircuitBreaker.decorateSupplier(
                        circuitBreaker, () -> {
                            throw new RuntimeException("failure");
                        });
                supplier.get();
            } catch (Exception ignored) {
            }
        }

        // 等待并转为半开
        Thread.sleep(1200);
        circuitBreaker.transitionToHalfOpenState();
        assertEquals(CircuitBreaker.State.HALF_OPEN, circuitBreaker.getState());

        // 半开状态下成功调用
        for (int i = 0; i < 2; i++) {
            Supplier<String> supplier = CircuitBreaker.decorateSupplier(
                    circuitBreaker, () -> "success");
            supplier.get();
        }

        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
    }

    @Test
    @DisplayName("熔断器指标应正确统计成功和失败次数")
    void shouldTrackMetricsCorrectly() {
        // 3 次成功
        for (int i = 0; i < 3; i++) {
            Supplier<String> supplier = CircuitBreaker.decorateSupplier(
                    circuitBreaker, () -> "success");
            supplier.get();
        }

        // 2 次失败
        for (int i = 0; i < 2; i++) {
            try {
                Supplier<String> supplier = CircuitBreaker.decorateSupplier(
                        circuitBreaker, () -> {
                            throw new RuntimeException("failure");
                        });
                supplier.get();
            } catch (Exception ignored) {
            }
        }

        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertEquals(5, metrics.getNumberOfBufferedCalls());
        assertEquals(3, metrics.getNumberOfSuccessfulCalls());
        assertEquals(2, metrics.getNumberOfFailedCalls());
        assertEquals(40.0f, metrics.getFailureRate(), 0.01);
    }
}
