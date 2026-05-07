package com.example.distributed.ratelimit;

import java.util.Deque;
import java.util.LinkedList;

/**
 * 限流算法实现演示
 *
 * 包含三种可运行的限流算法实现：
 * 1. 令牌桶算法（Token Bucket）
 * 2. 漏桶算法（Leaky Bucket）
 * 3. 滑动窗口算法（Sliding Window）
 */
public class RateLimitDemo {

    // ==================== 令牌桶算法 ====================

    /**
     * 令牌桶算法
     *
     * 原理：以固定速率向桶中放入令牌，请求需要获取令牌才能被处理。
     * 桶满时多余的令牌被丢弃。
     *
     * 特点：允许一定程度的突发流量（桶中积累的令牌可以一次性消耗）
     * 典型实现：Guava RateLimiter
     */
    public static class TokenBucketRateLimiter {
        private final long capacity;        // 桶容量（最大令牌数）
        private final long refillRate;      // 每秒放入令牌数
        private long tokens;                // 当前令牌数
        private long lastRefillTimeNanos;   // 上次填充时间（纳秒）

        public TokenBucketRateLimiter(long capacity, long refillRate) {
            this.capacity = capacity;
            this.refillRate = refillRate;
            this.tokens = capacity;  // 初始满桶
            this.lastRefillTimeNanos = System.nanoTime();
        }

        /**
         * 尝试获取一个令牌
         *
         * @return true=获取成功（允许请求），false=获取失败（限流）
         */
        public synchronized boolean tryAcquire() {
            refill();
            if (tokens > 0) {
                tokens--;
                return true;
            }
            return false;
        }

        /**
         * 获取当前令牌数（用于测试）
         */
        public synchronized long getTokens() {
            refill();
            return tokens;
        }

        /**
         * 根据时间流逝补充令牌
         */
        private void refill() {
            long now = System.nanoTime();
            long elapsed = now - lastRefillTimeNanos;
            // 计算应该补充的令牌数
            long newTokens = elapsed * refillRate / 1_000_000_000L;
            if (newTokens > 0) {
                tokens = Math.min(capacity, tokens + newTokens);
                lastRefillTimeNanos = now;
            }
        }
    }

    // ==================== 漏桶算法 ====================

    /**
     * 漏桶算法
     *
     * 原理：请求先进入桶中排队，以固定速率从桶中流出处理。
     * 桶满则拒绝新请求。
     *
     * 特点：输出速率恒定，无法应对突发流量
     * 典型应用：Nginx limit_req
     */
    public static class LeakyBucketRateLimiter {
        private final long capacity;        // 桶容量
        private final long leakRate;        // 每秒漏出数量
        private long water;                 // 当前水量（待处理请求数）
        private long lastLeakTimeNanos;     // 上次漏水时间

        public LeakyBucketRateLimiter(long capacity, long leakRate) {
            this.capacity = capacity;
            this.leakRate = leakRate;
            this.water = 0;
            this.lastLeakTimeNanos = System.nanoTime();
        }

        /**
         * 尝试添加一个请求到桶中
         *
         * @return true=添加成功（请求被接受），false=桶满（限流）
         */
        public synchronized boolean tryAcquire() {
            leak();
            if (water < capacity) {
                water++;
                return true;
            }
            return false;  // 桶满，拒绝
        }

        /**
         * 获取当前水量（用于测试）
         */
        public synchronized long getWater() {
            leak();
            return water;
        }

        /**
         * 根据时间流逝漏水
         */
        private void leak() {
            long now = System.nanoTime();
            long elapsed = now - lastLeakTimeNanos;
            long leaked = elapsed * leakRate / 1_000_000_000L;
            if (leaked > 0) {
                water = Math.max(0, water - leaked);
                lastLeakTimeNanos = now;
            }
        }
    }

    // ==================== 滑动窗口算法 ====================

    /**
     * 滑动窗口限流算法
     *
     * 原理：记录每个请求的时间戳，统计当前时间往前一个窗口内的请求数。
     * 超过阈值则拒绝。
     *
     * 特点：精确度高，解决了固定窗口的临界突刺问题
     * 典型实现：Redis ZSET + 时间戳
     */
    public static class SlidingWindowRateLimiter {
        private final int maxRequests;       // 窗口内最大请求数
        private final long windowSizeMs;     // 窗口大小（毫秒）
        private final Deque<Long> timestamps = new LinkedList<>();

        public SlidingWindowRateLimiter(int maxRequests, long windowSizeMs) {
            this.maxRequests = maxRequests;
            this.windowSizeMs = windowSizeMs;
        }

        /**
         * 尝试通过限流检查
         *
         * @return true=通过（允许请求），false=限流
         */
        public synchronized boolean tryAcquire() {
            long now = System.currentTimeMillis();
            // 移除窗口外的时间戳
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() > windowSizeMs) {
                timestamps.pollFirst();
            }
            if (timestamps.size() < maxRequests) {
                timestamps.addLast(now);
                return true;
            }
            return false;
        }

        /**
         * 获取当前窗口内的请求数（用于测试）
         */
        public synchronized int getCurrentCount() {
            long now = System.currentTimeMillis();
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() > windowSizeMs) {
                timestamps.pollFirst();
            }
            return timestamps.size();
        }
    }

    // ==================== 演示 ====================

    public static void main(String[] args) throws InterruptedException {
        System.out.println("========== 限流算法演示 ==========");
        System.out.println();

        // 1. 令牌桶演示
        System.out.println("--- 令牌桶算法（容量=5，每秒补充=2）---");
        TokenBucketRateLimiter tokenBucket = new TokenBucketRateLimiter(5, 2);
        System.out.println("初始令牌数: " + tokenBucket.getTokens());
        for (int i = 1; i <= 7; i++) {
            boolean allowed = tokenBucket.tryAcquire();
            System.out.println("  请求 " + i + ": " + (allowed ? "✅ 通过" : "❌ 限流"));
        }
        System.out.println("等待 1 秒补充令牌...");
        Thread.sleep(1000);
        System.out.println("当前令牌数: " + tokenBucket.getTokens());
        System.out.println();

        // 2. 漏桶演示
        System.out.println("--- 漏桶算法（容量=5，每秒漏出=2）---");
        LeakyBucketRateLimiter leakyBucket = new LeakyBucketRateLimiter(5, 2);
        for (int i = 1; i <= 7; i++) {
            boolean allowed = leakyBucket.tryAcquire();
            System.out.println("  请求 " + i + ": " + (allowed ? "✅ 通过" : "❌ 限流"));
        }
        System.out.println("等待 1 秒漏水...");
        Thread.sleep(1000);
        System.out.println("当前水量: " + leakyBucket.getWater());
        System.out.println();

        // 3. 滑动窗口演示
        System.out.println("--- 滑动窗口算法（窗口=1秒，最大=5请求）---");
        SlidingWindowRateLimiter slidingWindow = new SlidingWindowRateLimiter(5, 1000);
        for (int i = 1; i <= 7; i++) {
            boolean allowed = slidingWindow.tryAcquire();
            System.out.println("  请求 " + i + ": " + (allowed ? "✅ 通过" : "❌ 限流"));
        }
        System.out.println("等待 1 秒窗口滑动...");
        Thread.sleep(1100);
        System.out.println("当前窗口请求数: " + slidingWindow.getCurrentCount());
        boolean allowed = slidingWindow.tryAcquire();
        System.out.println("  新请求: " + (allowed ? "✅ 通过" : "❌ 限流"));
        System.out.println();

        // 对比总结
        System.out.println("========== 算法对比 ==========");
        System.out.println("| 算法     | 突发流量 | 输出特性 | 典型实现            |");
        System.out.println("|---------|---------|---------|-------------------|");
        System.out.println("| 令牌桶   | 允许突发 | 弹性输出 | Guava RateLimiter |");
        System.out.println("| 漏桶     | 平滑处理 | 恒定输出 | Nginx limit_req   |");
        System.out.println("| 滑动窗口 | 精确控制 | 窗口限制 | Redis ZSET        |");
    }
}
