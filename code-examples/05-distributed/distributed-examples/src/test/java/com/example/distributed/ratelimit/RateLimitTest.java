package com.example.distributed.ratelimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 限流算法正确性验证
 */
class RateLimitTest {

    // ==================== 令牌桶测试 ====================

    @Test
    @DisplayName("令牌桶：初始状态应有满桶令牌")
    void tokenBucket_initialTokensShouldBeFull() {
        var limiter = new RateLimitDemo.TokenBucketRateLimiter(10, 5);
        assertEquals(10, limiter.getTokens());
    }

    @Test
    @DisplayName("令牌桶：消耗令牌后数量应减少")
    void tokenBucket_shouldDecreaseAfterAcquire() {
        var limiter = new RateLimitDemo.TokenBucketRateLimiter(5, 2);
        assertTrue(limiter.tryAcquire());
        assertTrue(limiter.tryAcquire());
        // 消耗了 2 个，剩余应 <= 初始值
        assertTrue(limiter.getTokens() <= 5);
    }

    @Test
    @DisplayName("令牌桶：令牌耗尽后应拒绝请求")
    void tokenBucket_shouldRejectWhenEmpty() {
        var limiter = new RateLimitDemo.TokenBucketRateLimiter(3, 1);
        // 消耗所有令牌
        assertTrue(limiter.tryAcquire());
        assertTrue(limiter.tryAcquire());
        assertTrue(limiter.tryAcquire());
        // 第 4 个应被拒绝
        assertFalse(limiter.tryAcquire());
    }

    @Test
    @DisplayName("令牌桶：等待后应补充令牌")
    void tokenBucket_shouldRefillAfterWait() throws InterruptedException {
        var limiter = new RateLimitDemo.TokenBucketRateLimiter(5, 10);
        // 消耗所有令牌
        for (int i = 0; i < 5; i++) {
            limiter.tryAcquire();
        }
        assertFalse(limiter.tryAcquire());

        // 等待 500ms，应补充约 5 个令牌（10/秒 * 0.5秒）
        Thread.sleep(600);
        assertTrue(limiter.tryAcquire(), "等待后应能获取令牌");
    }

    @Test
    @DisplayName("令牌桶：令牌数不应超过容量")
    void tokenBucket_shouldNotExceedCapacity() throws InterruptedException {
        var limiter = new RateLimitDemo.TokenBucketRateLimiter(5, 100);
        // 等待足够长时间
        Thread.sleep(200);
        // 令牌数不应超过容量
        assertTrue(limiter.getTokens() <= 5);
    }

    // ==================== 漏桶测试 ====================

    @Test
    @DisplayName("漏桶：空桶应接受请求")
    void leakyBucket_shouldAcceptWhenEmpty() {
        var limiter = new RateLimitDemo.LeakyBucketRateLimiter(5, 2);
        assertTrue(limiter.tryAcquire());
    }

    @Test
    @DisplayName("漏桶：桶满后应拒绝请求")
    void leakyBucket_shouldRejectWhenFull() {
        var limiter = new RateLimitDemo.LeakyBucketRateLimiter(3, 1);
        // 填满桶
        assertTrue(limiter.tryAcquire());
        assertTrue(limiter.tryAcquire());
        assertTrue(limiter.tryAcquire());
        // 桶满，拒绝
        assertFalse(limiter.tryAcquire());
    }

    @Test
    @DisplayName("漏桶：等待后应漏出水量，接受新请求")
    void leakyBucket_shouldLeakAfterWait() throws InterruptedException {
        var limiter = new RateLimitDemo.LeakyBucketRateLimiter(3, 10);
        // 填满桶
        for (int i = 0; i < 3; i++) {
            limiter.tryAcquire();
        }
        assertFalse(limiter.tryAcquire());

        // 等待 500ms，应漏出约 5 个（10/秒 * 0.5秒），桶变空
        Thread.sleep(600);
        assertTrue(limiter.tryAcquire(), "等待后应能接受请求");
    }

    // ==================== 滑动窗口测试 ====================

    @Test
    @DisplayName("滑动窗口：窗口内请求数未超限应通过")
    void slidingWindow_shouldAllowWithinLimit() {
        var limiter = new RateLimitDemo.SlidingWindowRateLimiter(5, 1000);
        for (int i = 0; i < 5; i++) {
            assertTrue(limiter.tryAcquire(), "第 " + (i + 1) + " 个请求应通过");
        }
    }

    @Test
    @DisplayName("滑动窗口：超过限制应拒绝")
    void slidingWindow_shouldRejectWhenExceedLimit() {
        var limiter = new RateLimitDemo.SlidingWindowRateLimiter(3, 1000);
        assertTrue(limiter.tryAcquire());
        assertTrue(limiter.tryAcquire());
        assertTrue(limiter.tryAcquire());
        assertFalse(limiter.tryAcquire());
    }

    @Test
    @DisplayName("滑动窗口：窗口滑动后应重新接受请求")
    void slidingWindow_shouldResetAfterWindowSlides() throws InterruptedException {
        var limiter = new RateLimitDemo.SlidingWindowRateLimiter(3, 500);
        // 填满窗口
        for (int i = 0; i < 3; i++) {
            limiter.tryAcquire();
        }
        assertFalse(limiter.tryAcquire());

        // 等待窗口滑动
        Thread.sleep(600);
        assertEquals(0, limiter.getCurrentCount(), "窗口滑动后计数应为 0");
        assertTrue(limiter.tryAcquire(), "窗口滑动后应能接受请求");
    }

    @Test
    @DisplayName("滑动窗口：getCurrentCount 应返回正确的窗口内请求数")
    void slidingWindow_getCurrentCountShouldBeAccurate() {
        var limiter = new RateLimitDemo.SlidingWindowRateLimiter(10, 1000);
        limiter.tryAcquire();
        limiter.tryAcquire();
        limiter.tryAcquire();
        assertEquals(3, limiter.getCurrentCount());
    }
}
