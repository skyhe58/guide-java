package com.example.springcloud.circuitbreaker;

/**
 * 熔断器演示 — 用状态机实现 CLOSED/OPEN/HALF_OPEN 三态转换
 *
 * <p>本示例用纯 Java 模拟 Resilience4j 熔断器的核心机制：
 * <ul>
 *   <li>三种状态：CLOSED（正常）→ OPEN（熔断）→ HALF_OPEN（半开探测）</li>
 *   <li>滑动窗口：基于计数 / 基于时间</li>
 *   <li>失败率阈值触发熔断</li>
 *   <li>半开状态探测恢复</li>
 * </ul>
 *
 * <h3>熔断器状态转换：</h3>
 * <pre>
 *  CLOSED（正常通行）
 *    │ 失败率 >= 阈值（如 50%）
 *    ↓
 *  OPEN（拒绝所有请求，快速失败）
 *    │ 等待超时（如 10s）
 *    ↓
 *  HALF_OPEN（放行少量探测请求）
 *    │ 探测成功 → 回到 CLOSED
 *    │ 探测失败 → 回到 OPEN
 * </pre>
 */
public class CircuitBreakerDemo {

    enum State { CLOSED, OPEN, HALF_OPEN }

    /** 熔断器实现 */
    static class CircuitBreaker {
        private final String name;
        private final int windowSize;           // 滑动窗口大小
        private final double failureThreshold;  // 失败率阈值（0.0~1.0）
        private final long openTimeoutMs;       // OPEN 状态持续时间
        private final int halfOpenPermits;      // HALF_OPEN 允许的探测请求数

        private volatile State state = State.CLOSED;
        private final java.util.LinkedList<Boolean> window = new java.util.LinkedList<>(); // true=成功, false=失败
        private long openedAt;                  // 进入 OPEN 状态的时间
        private int halfOpenAttempts;           // HALF_OPEN 已探测次数
        private int halfOpenSuccesses;          // HALF_OPEN 探测成功次数

        // 统计
        private long totalRequests = 0;
        private long rejectedRequests = 0;

        CircuitBreaker(String name, int windowSize, double failureThreshold,
                       long openTimeoutMs, int halfOpenPermits) {
            this.name = name;
            this.windowSize = windowSize;
            this.failureThreshold = failureThreshold;
            this.openTimeoutMs = openTimeoutMs;
            this.halfOpenPermits = halfOpenPermits;
        }

        /** 执行请求（带熔断保护） */
        <T> T execute(java.util.concurrent.Callable<T> callable, java.util.function.Supplier<T> fallback) {
            totalRequests++;

            // 状态检查
            if (state == State.OPEN) {
                if (System.currentTimeMillis() - openedAt >= openTimeoutMs) {
                    transitionTo(State.HALF_OPEN);
                } else {
                    rejectedRequests++;
                    return fallback.get(); // 快速失败
                }
            }

            if (state == State.HALF_OPEN && halfOpenAttempts >= halfOpenPermits) {
                rejectedRequests++;
                return fallback.get();
            }

            // 执行请求
            try {
                T result = callable.call();
                recordSuccess();
                return result;
            } catch (Exception e) {
                recordFailure();
                return fallback.get();
            }
        }

        private void recordSuccess() {
            if (state == State.HALF_OPEN) {
                halfOpenAttempts++;
                halfOpenSuccesses++;
                if (halfOpenAttempts >= halfOpenPermits) {
                    if ((double) halfOpenSuccesses / halfOpenAttempts >= (1 - failureThreshold)) {
                        transitionTo(State.CLOSED); // 探测成功，恢复
                    } else {
                        transitionTo(State.OPEN);   // 探测失败，继续熔断
                    }
                }
            } else {
                addToWindow(true);
            }
        }

        private void recordFailure() {
            if (state == State.HALF_OPEN) {
                halfOpenAttempts++;
                if (halfOpenAttempts >= halfOpenPermits) {
                    transitionTo(State.OPEN);
                }
            } else {
                addToWindow(false);
                checkThreshold();
            }
        }

        private void addToWindow(boolean success) {
            window.addLast(success);
            if (window.size() > windowSize) window.removeFirst();
        }

        private void checkThreshold() {
            if (window.size() >= windowSize) {
                long failures = window.stream().filter(b -> !b).count();
                double failureRate = (double) failures / window.size();
                if (failureRate >= failureThreshold) {
                    transitionTo(State.OPEN);
                }
            }
        }

        private void transitionTo(State newState) {
            State oldState = this.state;
            this.state = newState;
            if (newState == State.OPEN) {
                openedAt = System.currentTimeMillis();
            } else if (newState == State.HALF_OPEN) {
                halfOpenAttempts = 0;
                halfOpenSuccesses = 0;
            } else if (newState == State.CLOSED) {
                window.clear();
            }
            System.out.printf("    [%s] 状态转换: %s → %s%n", name, oldState, newState);
        }

        State getState() { return state; }
        double getFailureRate() {
            if (window.isEmpty()) return 0;
            return (double) window.stream().filter(b -> !b).count() / window.size();
        }
        String getStats() {
            return String.format("state=%s, requests=%d, rejected=%d, failureRate=%.0f%%",
                    state, totalRequests, rejectedRequests, getFailureRate() * 100);
        }
    }

    // ==================== 演示方法 ====================

    /** 演示1：熔断器三态转换 */
    static void demoStateTransition() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示1：熔断器三态转换");
        System.out.println("═══════════════════════════════════════════════════");

        // 窗口大小 10，失败率阈值 50%，OPEN 持续 500ms，HALF_OPEN 探测 3 次
        CircuitBreaker cb = new CircuitBreaker("user-service", 10, 0.5, 500, 3);

        System.out.printf("\n  初始状态: %s%n", cb.getStats());

        // 正常请求
        System.out.println("\n  【阶段1：正常请求（CLOSED）】");
        for (int i = 0; i < 5; i++) {
            cb.execute(() -> "OK", () -> "FALLBACK");
        }
        System.out.printf("  状态: %s%n", cb.getStats());

        // 连续失败 → 触发熔断
        System.out.println("\n  【阶段2：连续失败 → 触发熔断（CLOSED → OPEN）】");
        for (int i = 0; i < 6; i++) {
            cb.execute(() -> { throw new RuntimeException("服务超时"); }, () -> "FALLBACK");
        }
        System.out.printf("  状态: %s%n", cb.getStats());

        // OPEN 状态：请求被拒绝
        System.out.println("\n  【阶段3：OPEN 状态 — 请求被快速拒绝】");
        for (int i = 0; i < 3; i++) {
            String result = cb.execute(() -> "OK", () -> "FALLBACK(熔断中)");
            System.out.printf("    请求 %d: %s%n", i + 1, result);
        }
        System.out.printf("  状态: %s%n", cb.getStats());

        // 等待超时 → HALF_OPEN
        System.out.println("\n  【阶段4：等待超时 → HALF_OPEN 探测】");
        try { Thread.sleep(600); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        // HALF_OPEN 探测成功 → 恢复 CLOSED
        for (int i = 0; i < 3; i++) {
            cb.execute(() -> "OK", () -> "FALLBACK");
        }
        System.out.printf("  状态: %s%n", cb.getStats());
        System.out.println();
    }

    /** 演示2：Resilience4j 配置示例 */
    static void demoConfiguration() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示2：Resilience4j 配置");
        System.out.println("═══════════════════════════════════════════════════");

        System.out.println("\n  application.yml 配置：");
        System.out.println("  resilience4j:");
        System.out.println("    circuitbreaker:");
        System.out.println("      instances:");
        System.out.println("        userService:");
        System.out.println("          slidingWindowType: COUNT_BASED     # 基于计数的滑动窗口");
        System.out.println("          slidingWindowSize: 10              # 窗口大小");
        System.out.println("          failureRateThreshold: 50           # 失败率阈值 50%");
        System.out.println("          waitDurationInOpenState: 10s       # OPEN 持续时间");
        System.out.println("          permittedNumberOfCallsInHalfOpenState: 3  # HALF_OPEN 探测数");
        System.out.println("          minimumNumberOfCalls: 5            # 最少调用次数才计算失败率");

        System.out.println("\n  代码使用：");
        System.out.println("    @CircuitBreaker(name = \"userService\", fallbackMethod = \"fallback\")");
        System.out.println("    public User getUser(Long id) { ... }");
        System.out.println("    public User fallback(Long id, Exception e) { return defaultUser; }");
        System.out.println();
    }

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  熔断器演示 — 状态机模拟 Resilience4j（纯内存）        ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        demoStateTransition();
        demoConfiguration();
    }
}
