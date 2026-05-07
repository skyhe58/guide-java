package com.example.redis.lock;

import com.example.redis.lock.DistributedLockDemo.ReentrantRedisLock;
import com.example.redis.lock.DistributedLockDemo.SimpleRedisLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis 分布式锁逻辑验证测试
 *
 * <p>不依赖实际 Redis 连接，测试锁的核心逻辑：</p>
 * <ul>
 *   <li>互斥性：同一时刻只有一个线程持有锁</li>
 *   <li>防误删：只能释放自己的锁</li>
 *   <li>可重入：同一线程可重复获取锁</li>
 *   <li>并发安全：多线程竞争下锁的正确性</li>
 * </ul>
 */
class DistributedLockTest {

    // ==================== 简单分布式锁测试 ====================

    @Nested
    @DisplayName("SimpleRedisLock 测试")
    class SimpleRedisLockTest {

        private SimpleRedisLock lock;

        @BeforeEach
        void setUp() {
            lock = new SimpleRedisLock();
        }

        @Test
        @DisplayName("加锁成功后锁应处于持有状态")
        void shouldLockSuccessfully() {
            String key = "order:1001";
            String owner = UUID.randomUUID().toString();

            boolean result = lock.tryLock(key, owner, 30);

            assertThat(result).isTrue();
            assertThat(lock.isLocked(key)).isTrue();
            assertThat(lock.getLockOwner(key)).isEqualTo(owner);
        }

        @Test
        @DisplayName("锁已被持有时其他线程加锁应失败")
        void shouldFailWhenLockAlreadyHeld() {
            String key = "order:1001";
            String owner1 = "thread-1";
            String owner2 = "thread-2";

            lock.tryLock(key, owner1, 30);
            boolean result = lock.tryLock(key, owner2, 30);

            assertThat(result).isFalse();
            assertThat(lock.getLockOwner(key)).isEqualTo(owner1);
        }

        @Test
        @DisplayName("只能释放自己持有的锁（防误删）")
        void shouldOnlyUnlockOwnLock() {
            String key = "order:1001";
            String owner1 = "thread-1";
            String owner2 = "thread-2";

            lock.tryLock(key, owner1, 30);

            // 其他线程尝试释放 → 失败
            boolean unlockByOther = lock.unlock(key, owner2);
            assertThat(unlockByOther).isFalse();
            assertThat(lock.isLocked(key)).isTrue();

            // 持有者释放 → 成功
            boolean unlockByOwner = lock.unlock(key, owner1);
            assertThat(unlockByOwner).isTrue();
            assertThat(lock.isLocked(key)).isFalse();
        }

        @Test
        @DisplayName("释放锁后其他线程可以加锁")
        void shouldAllowLockAfterUnlock() {
            String key = "order:1001";
            String owner1 = "thread-1";
            String owner2 = "thread-2";

            lock.tryLock(key, owner1, 30);
            lock.unlock(key, owner1);

            boolean result = lock.tryLock(key, owner2, 30);
            assertThat(result).isTrue();
            assertThat(lock.getLockOwner(key)).isEqualTo(owner2);
        }

        @Test
        @DisplayName("锁过期后应自动释放")
        void shouldExpireAutomatically() throws InterruptedException {
            String key = "order:1001";
            String owner1 = "thread-1";
            String owner2 = "thread-2";

            // 设置 1 秒过期
            lock.tryLock(key, owner1, 1);
            assertThat(lock.isLocked(key)).isTrue();

            // 等待过期
            Thread.sleep(1100);

            // 过期后其他线程可以加锁
            assertThat(lock.isLocked(key)).isFalse();
            boolean result = lock.tryLock(key, owner2, 30);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("多线程并发加锁只有一个成功（互斥性）")
        void shouldEnsureMutualExclusion() throws InterruptedException {
            String key = "order:concurrent";
            int threadCount = 10;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);

            for (int i = 0; i < threadCount; i++) {
                final String owner = "thread-" + i;
                new Thread(() -> {
                    try {
                        startLatch.await();
                        if (lock.tryLock(key, owner, 30)) {
                            successCount.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                }).start();
            }

            startLatch.countDown();  // 同时开始
            doneLatch.await();       // 等待全部完成

            assertThat(successCount.get()).isEqualTo(1);
        }

        @Test
        @DisplayName("不同 key 的锁互不影响")
        void shouldNotAffectDifferentKeys() {
            String owner = "thread-1";

            boolean lock1 = lock.tryLock("order:1001", owner, 30);
            boolean lock2 = lock.tryLock("order:1002", owner, 30);

            assertThat(lock1).isTrue();
            assertThat(lock2).isTrue();
        }
    }

    // ==================== 可重入分布式锁测试 ====================

    @Nested
    @DisplayName("ReentrantRedisLock 测试")
    class ReentrantRedisLockTest {

        private ReentrantRedisLock lock;

        @BeforeEach
        void setUp() {
            lock = new ReentrantRedisLock();
        }

        @Test
        @DisplayName("同一持有者可重入加锁")
        void shouldSupportReentrant() {
            String key = "inventory:2001";
            String owner = "uuid:thread-1";

            boolean first = lock.tryLock(key, owner, 30);
            boolean second = lock.tryLock(key, owner, 30);

            assertThat(first).isTrue();
            assertThat(second).isTrue();
            assertThat(lock.getHoldCount(key, owner)).isEqualTo(2);
        }

        @Test
        @DisplayName("重入后需要释放相同次数才能完全释放")
        void shouldRequireMatchingUnlocks() {
            String key = "inventory:2001";
            String owner = "uuid:thread-1";

            lock.tryLock(key, owner, 30);
            lock.tryLock(key, owner, 30);
            lock.tryLock(key, owner, 30);
            assertThat(lock.getHoldCount(key, owner)).isEqualTo(3);

            lock.unlock(key, owner);
            assertThat(lock.getHoldCount(key, owner)).isEqualTo(2);

            lock.unlock(key, owner);
            assertThat(lock.getHoldCount(key, owner)).isEqualTo(1);

            lock.unlock(key, owner);
            assertThat(lock.getHoldCount(key, owner)).isEqualTo(0);
        }

        @Test
        @DisplayName("不同持有者不能重入")
        void shouldNotAllowDifferentOwnerReentrant() {
            String key = "inventory:2001";
            String owner1 = "uuid:thread-1";
            String owner2 = "uuid:thread-2";

            boolean first = lock.tryLock(key, owner1, 30);
            boolean second = lock.tryLock(key, owner2, 30);

            assertThat(first).isTrue();
            assertThat(second).isFalse();
        }

        @Test
        @DisplayName("完全释放后其他持有者可以加锁")
        void shouldAllowOtherOwnerAfterFullRelease() {
            String key = "inventory:2001";
            String owner1 = "uuid:thread-1";
            String owner2 = "uuid:thread-2";

            lock.tryLock(key, owner1, 30);
            lock.tryLock(key, owner1, 30);

            lock.unlock(key, owner1);
            // 还没完全释放，owner2 不能加锁
            boolean locked = lock.tryLock(key, owner2, 30);
            assertThat(locked).isFalse();

            lock.unlock(key, owner1);
            // 完全释放后，owner2 可以加锁
            locked = lock.tryLock(key, owner2, 30);
            assertThat(locked).isTrue();
        }

        @Test
        @DisplayName("非持有者释放锁应失败")
        void shouldFailUnlockByNonOwner() {
            String key = "inventory:2001";
            String owner1 = "uuid:thread-1";
            String owner2 = "uuid:thread-2";

            lock.tryLock(key, owner1, 30);

            boolean result = lock.unlock(key, owner2);
            assertThat(result).isFalse();
            assertThat(lock.getHoldCount(key, owner1)).isEqualTo(1);
        }
    }
}
