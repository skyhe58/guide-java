package com.example.database.lock;

/**
 * MySQL 锁机制演示 — 用 Java 并发工具模拟 InnoDB 的各种锁
 *
 * <p>本示例模拟 MySQL InnoDB 引擎的锁机制：
 * <ul>
 *   <li>行锁（Record Lock）— 锁定索引上的单行记录</li>
 *   <li>间隙锁（Gap Lock）— 锁定索引记录之间的间隙，防止幻读</li>
 *   <li>临键锁（Next-Key Lock）— 行锁 + 间隙锁的组合，左开右闭区间</li>
 *   <li>表锁 vs 行锁的对比</li>
 *   <li>意向锁（Intention Lock）— 表级锁，用于快速判断表中是否有行锁</li>
 *   <li>死锁检测与避免</li>
 * </ul>
 *
 * <p>如需连接真实 MySQL，使用：
 * {@code docker compose -f docker/docker-compose.yml up -d mysql}</p>
 *
 * <h3>InnoDB 锁类型层级：</h3>
 * <pre>
 *  表级锁
 *  ├── 表锁（LOCK TABLES）
 *  ├── 意向共享锁（IS）— 事务想获取行级共享锁前，先获取 IS
 *  └── 意向排他锁（IX）— 事务想获取行级排他锁前，先获取 IX
 *
 *  行级锁（基于索引实现）
 *  ├── 记录锁（Record Lock）— 锁定单条索引记录
 *  ├── 间隙锁（Gap Lock）— 锁定索引记录之间的间隙 (a, b)
 *  └── 临键锁（Next-Key Lock）— Record + Gap = (a, b]
 * </pre>
 */
public class LockDemo {

    // ==================== 锁模型 ====================

    /** 锁类型枚举 */
    enum LockType {
        RECORD_LOCK_SHARED,     // 行级共享锁（S 锁）— SELECT ... LOCK IN SHARE MODE
        RECORD_LOCK_EXCLUSIVE,  // 行级排他锁（X 锁）— SELECT ... FOR UPDATE / UPDATE / DELETE
        GAP_LOCK,               // 间隙锁 — 锁定间隙，防止插入
        NEXT_KEY_LOCK,          // 临键锁 — Record Lock + Gap Lock
        TABLE_LOCK_SHARED,      // 表级共享锁
        TABLE_LOCK_EXCLUSIVE    // 表级排他锁
    }

    /** 锁信息 */
    static class LockInfo {
        final String transactionId;
        final LockType type;
        final int recordId;     // 锁定的记录 ID（-1 表示表锁）
        final int gapStart;     // 间隙起始（仅 Gap/Next-Key Lock）
        final int gapEnd;       // 间隙结束

        LockInfo(String transactionId, LockType type, int recordId) {
            this(transactionId, type, recordId, -1, -1);
        }

        LockInfo(String transactionId, LockType type, int recordId, int gapStart, int gapEnd) {
            this.transactionId = transactionId;
            this.type = type;
            this.recordId = recordId;
            this.gapStart = gapStart;
            this.gapEnd = gapEnd;
        }

        @Override
        public String toString() {
            if (type == LockType.GAP_LOCK) {
                return String.format("[%s] %s 间隙(%d, %d)", transactionId, type, gapStart, gapEnd);
            } else if (type == LockType.NEXT_KEY_LOCK) {
                return String.format("[%s] %s (%d, %d]", transactionId, type, gapStart, gapEnd);
            }
            return String.format("[%s] %s record=%d", transactionId, type, recordId);
        }
    }

    // ==================== 行锁管理器 ====================

    /**
     * 模拟 InnoDB 的行锁管理器。
     * InnoDB 的行锁是加在索引上的，不是加在数据行上。
     * 如果没有索引，行锁会退化为表锁。
     */
    static class LockManager {
        // recordId → 持有锁的事务列表
        private final java.util.Map<Integer, java.util.List<LockInfo>> lockTable =
                new java.util.concurrent.ConcurrentHashMap<>();
        // 等待队列
        private final java.util.List<String> waitQueue =
                java.util.Collections.synchronizedList(new java.util.ArrayList<>());

        /**
         * 尝试获取行锁。
         * 兼容性矩阵：
         *   S 锁 + S 锁 = 兼容（多个事务可以同时读）
         *   S 锁 + X 锁 = 冲突（读写互斥）
         *   X 锁 + X 锁 = 冲突（写写互斥）
         */
        boolean tryLock(String trxId, int recordId, LockType type) {
            java.util.List<LockInfo> locks = lockTable.computeIfAbsent(recordId,
                    k -> java.util.Collections.synchronizedList(new java.util.ArrayList<>()));

            synchronized (locks) {
                for (LockInfo existing : locks) {
                    if (existing.transactionId.equals(trxId)) continue; // 同一事务的锁不冲突

                    // 检查锁兼容性
                    if (!isCompatible(existing.type, type)) {
                        waitQueue.add(String.format("%s 等待 record=%d 的 %s（被 %s 的 %s 阻塞）",
                                trxId, recordId, type, existing.transactionId, existing.type));
                        return false;
                    }
                }
                locks.add(new LockInfo(trxId, type, recordId));
                return true;
            }
        }

        /** 释放事务持有的所有锁 */
        void releaseLocks(String trxId) {
            for (java.util.List<LockInfo> locks : lockTable.values()) {
                synchronized (locks) {
                    locks.removeIf(l -> l.transactionId.equals(trxId));
                }
            }
        }

        /** 锁兼容性判断 */
        private boolean isCompatible(LockType existing, LockType requested) {
            // S + S = 兼容
            if (existing == LockType.RECORD_LOCK_SHARED && requested == LockType.RECORD_LOCK_SHARED) {
                return true;
            }
            // 其他组合都冲突（S+X, X+S, X+X）
            return false;
        }

        java.util.List<String> getWaitQueue() {
            return new java.util.ArrayList<>(waitQueue);
        }

        void printLockTable() {
            for (var entry : lockTable.entrySet()) {
                for (LockInfo lock : entry.getValue()) {
                    System.out.println("    " + lock);
                }
            }
        }
    }

    // ==================== 演示方法 ====================

    /** 演示1：行锁 — 共享锁与排他锁 */
    static void demoRecordLock() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示1：行锁 — 共享锁(S) 与 排他锁(X)");
        System.out.println("═══════════════════════════════════════════════════");

        LockManager lm = new LockManager();

        // 场景1：两个事务同时加 S 锁 → 兼容
        System.out.println("\n  【场景1】两个事务对同一行加 S 锁（兼容）");
        System.out.println("  SQL: SELECT * FROM user WHERE id=1 LOCK IN SHARE MODE");
        boolean r1 = lm.tryLock("trxA", 1, LockType.RECORD_LOCK_SHARED);
        boolean r2 = lm.tryLock("trxB", 1, LockType.RECORD_LOCK_SHARED);
        System.out.printf("    事务A 加 S 锁: %s%n", r1 ? "成功" : "阻塞");
        System.out.printf("    事务B 加 S 锁: %s（S+S 兼容，多个事务可同时读）%n", r2 ? "成功" : "阻塞");
        lm.releaseLocks("trxA");
        lm.releaseLocks("trxB");

        // 场景2：S 锁 + X 锁 → 冲突
        System.out.println("\n  【场景2】事务A持有 S 锁，事务B请求 X 锁（冲突）");
        System.out.println("  SQL: 事务A: SELECT ... LOCK IN SHARE MODE; 事务B: UPDATE ...");
        lm.tryLock("trxA", 1, LockType.RECORD_LOCK_SHARED);
        boolean r3 = lm.tryLock("trxB", 1, LockType.RECORD_LOCK_EXCLUSIVE);
        System.out.printf("    事务A 持有 S 锁%n");
        System.out.printf("    事务B 请求 X 锁: %s（S+X 冲突，事务B被阻塞）%n", r3 ? "成功" : "阻塞");
        lm.releaseLocks("trxA");
        lm.releaseLocks("trxB");

        // 场景3：X 锁 + X 锁 → 冲突
        System.out.println("\n  【场景3】两个事务对同一行加 X 锁（冲突）");
        System.out.println("  SQL: 两个事务同时 UPDATE user SET name='x' WHERE id=1");
        lm.tryLock("trxA", 1, LockType.RECORD_LOCK_EXCLUSIVE);
        boolean r4 = lm.tryLock("trxB", 1, LockType.RECORD_LOCK_EXCLUSIVE);
        System.out.printf("    事务A 加 X 锁: 成功%n");
        System.out.printf("    事务B 加 X 锁: %s（X+X 冲突，写写互斥）%n", r4 ? "成功" : "阻塞");
        lm.releaseLocks("trxA");
        lm.releaseLocks("trxB");

        // 场景4：不同行的锁互不影响
        System.out.println("\n  【场景4】不同行的锁互不影响");
        boolean r5 = lm.tryLock("trxA", 1, LockType.RECORD_LOCK_EXCLUSIVE);
        boolean r6 = lm.tryLock("trxB", 2, LockType.RECORD_LOCK_EXCLUSIVE);
        System.out.printf("    事务A 锁 id=1: %s%n", r5 ? "成功" : "阻塞");
        System.out.printf("    事务B 锁 id=2: %s（不同行，互不影响）%n", r6 ? "成功" : "阻塞");
        lm.releaseLocks("trxA");
        lm.releaseLocks("trxB");
        System.out.println();
    }

    /** 演示2：间隙锁与临键锁 — 防止幻读 */
    static void demoGapAndNextKeyLock() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示2：间隙锁(Gap Lock) 与 临键锁(Next-Key Lock)");
        System.out.println("═══════════════════════════════════════════════════");

        // 模拟索引上的记录：id = 5, 10, 15, 20
        Integer[] indexRecords = {5, 10, 15, 20};

        System.out.println("\n  索引上的记录: [5, 10, 15, 20]");
        System.out.println("  间隙(Gap): (-∞,5), (5,10), (10,15), (15,20), (20,+∞)");
        System.out.println();

        // 临键锁 = 间隙锁 + 记录锁，左开右闭
        System.out.println("  Next-Key Lock 划分（左开右闭）:");
        System.out.println("    (-∞, 5]  (5, 10]  (10, 15]  (15, 20]  (20, +∞)");
        System.out.println();

        // 场景：SELECT * FROM t WHERE id = 10 FOR UPDATE
        System.out.println("  【场景1】SELECT * FROM t WHERE id = 10 FOR UPDATE");
        System.out.println("  加锁范围：Next-Key Lock (5, 10] + Gap Lock (10, 15)");
        System.out.println("  效果：");
        System.out.println("    ✗ INSERT id=7  → 被间隙锁阻塞（在间隙 (5,10) 中）");
        System.out.println("    ✗ INSERT id=12 → 被间隙锁阻塞（在间隙 (10,15) 中）");
        System.out.println("    ✓ INSERT id=3  → 允许（不在锁定范围内）");
        System.out.println("    ✓ INSERT id=17 → 允许（不在锁定范围内）");

        // 场景：范围查询
        System.out.println("\n  【场景2】SELECT * FROM t WHERE id > 10 AND id < 20 FOR UPDATE");
        System.out.println("  加锁范围：Next-Key Lock (10, 15] + Next-Key Lock (15, 20]");
        System.out.println("  效果：id 在 (10, 20] 范围内的插入和修改都被阻塞");

        // 场景：等值查询不存在的记录
        System.out.println("\n  【场景3】SELECT * FROM t WHERE id = 12 FOR UPDATE（记录不存在）");
        System.out.println("  加锁范围：Gap Lock (10, 15)（只加间隙锁，不加记录锁）");
        System.out.println("  效果：阻止在 (10, 15) 间隙中插入新记录，防止幻读");

        // 用代码模拟间隙锁阻止插入
        System.out.println("\n  【代码模拟】间隙锁阻止插入：");
        java.util.TreeSet<Integer> index = new java.util.TreeSet<>(java.util.Arrays.asList(indexRecords));

        // 模拟 INSERT 时检查间隙锁
        int[] insertAttempts = {3, 7, 12, 17, 25};
        int gapStart = 5, gapEnd = 15; // 模拟锁定间隙 (5, 15)

        for (int id : insertAttempts) {
            boolean blocked = id > gapStart && id < gapEnd;
            System.out.printf("    INSERT id=%d → %s%n", id,
                    blocked ? "✗ 被间隙锁 (" + gapStart + "," + gapEnd + ") 阻塞" : "✓ 允许插入");
        }
        System.out.println();
    }

    /** 演示3：表锁 vs 行锁 */
    static void demoTableVsRowLock() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示3：表锁 vs 行锁");
        System.out.println("═══════════════════════════════════════════════════");

        // 用 ReadWriteLock 模拟表锁
        java.util.concurrent.locks.ReentrantReadWriteLock tableLock =
                new java.util.concurrent.locks.ReentrantReadWriteLock();

        // 用 ConcurrentHashMap 模拟行锁
        java.util.concurrent.ConcurrentHashMap<Integer, java.util.concurrent.locks.ReentrantLock> rowLocks =
                new java.util.concurrent.ConcurrentHashMap<>();
        for (int i = 1; i <= 5; i++) {
            rowLocks.put(i, new java.util.concurrent.locks.ReentrantLock());
        }

        System.out.println("\n  【表锁】LOCK TABLES user WRITE");
        System.out.println("  特点：锁定整张表，其他事务无法读写任何行");
        System.out.println("  场景：MyISAM 引擎、ALTER TABLE、LOCK TABLES 语句");
        System.out.println("  并发度：极低（整张表串行）");

        System.out.println("\n  【行锁】SELECT * FROM user WHERE id=1 FOR UPDATE");
        System.out.println("  特点：只锁定匹配的行，其他行不受影响");
        System.out.println("  场景：InnoDB 引擎的 UPDATE/DELETE/SELECT FOR UPDATE");
        System.out.println("  并发度：高（不同行可以并行操作）");

        // 模拟并发对比
        System.out.println("\n  【并发对比模拟】5个线程同时操作不同行：");

        // 表锁：串行执行
        long start = System.nanoTime();
        for (int i = 1; i <= 5; i++) {
            tableLock.writeLock().lock();
            // 模拟操作
            tableLock.writeLock().unlock();
        }
        long tableLockTime = System.nanoTime() - start;

        // 行锁：可以并行
        start = System.nanoTime();
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(5);
        for (int i = 1; i <= 5; i++) {
            final int id = i;
            new Thread(() -> {
                rowLocks.get(id).lock();
                try {
                    // 模拟操作
                } finally {
                    rowLocks.get(id).unlock();
                    latch.countDown();
                }
            }).start();
        }
        try { latch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        long rowLockTime = System.nanoTime() - start;

        System.out.printf("    表锁耗时: %d ns（串行）%n", tableLockTime);
        System.out.printf("    行锁耗时: %d ns（并行）%n", rowLockTime);

        System.out.println("\n  重要：InnoDB 行锁是基于索引的！");
        System.out.println("  如果 WHERE 条件没有命中索引，行锁会退化为表锁");
        System.out.println("  例：UPDATE user SET age=20 WHERE name='张三'（name 无索引 → 表锁）");
        System.out.println();
    }

    /** 演示4：死锁检测与避免 */
    static void demoDeadlock() throws InterruptedException {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示4：死锁检测与避免");
        System.out.println("═══════════════════════════════════════════════════");

        java.util.concurrent.locks.ReentrantLock lockA = new java.util.concurrent.locks.ReentrantLock();
        java.util.concurrent.locks.ReentrantLock lockB = new java.util.concurrent.locks.ReentrantLock();
        java.util.concurrent.CountDownLatch ready = new java.util.concurrent.CountDownLatch(2);
        java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(2);

        System.out.println("\n  死锁场景：");
        System.out.println("  事务A: UPDATE account SET balance=balance-100 WHERE id=1（锁 id=1）");
        System.out.println("  事务A: UPDATE account SET balance=balance+100 WHERE id=2（等 id=2 的锁）");
        System.out.println("  事务B: UPDATE account SET balance=balance-50 WHERE id=2（锁 id=2）");
        System.out.println("  事务B: UPDATE account SET balance=balance+50 WHERE id=1（等 id=1 的锁）");
        System.out.println("  → 互相等待，形成死锁！");

        // 用 tryLock 模拟死锁检测
        System.out.println("\n  【模拟死锁检测】使用 tryLock 超时机制：");

        Thread t1 = new Thread(() -> {
            try {
                lockA.lock();
                System.out.println("  [事务A] 获取 id=1 的锁");
                ready.countDown();
                ready.await();
                Thread.sleep(50);

                // 尝试获取 lockB，设置超时
                boolean got = lockB.tryLock(200, java.util.concurrent.TimeUnit.MILLISECONDS);
                if (got) {
                    System.out.println("  [事务A] 获取 id=2 的锁 → 成功");
                    lockB.unlock();
                } else {
                    System.out.println("  [事务A] 获取 id=2 的锁 → 超时！检测到死锁，回滚事务A");
                }
                lockA.unlock();
                done.countDown();
            } catch (Exception e) { e.printStackTrace(); }
        });

        Thread t2 = new Thread(() -> {
            try {
                lockB.lock();
                System.out.println("  [事务B] 获取 id=2 的锁");
                ready.countDown();
                ready.await();
                Thread.sleep(50);

                boolean got = lockA.tryLock(200, java.util.concurrent.TimeUnit.MILLISECONDS);
                if (got) {
                    System.out.println("  [事务B] 获取 id=1 的锁 → 成功");
                    lockA.unlock();
                } else {
                    System.out.println("  [事务B] 获取 id=1 的锁 → 超时！检测到死锁，回滚事务B");
                }
                lockB.unlock();
                done.countDown();
            } catch (Exception e) { e.printStackTrace(); }
        });

        t1.start();
        t2.start();
        done.await();

        System.out.println("\n  MySQL 死锁处理策略：");
        System.out.println("  1. innodb_deadlock_detect=ON → 主动检测死锁，回滚代价小的事务");
        System.out.println("  2. innodb_lock_wait_timeout=50 → 超时等待，超时后回滚");
        System.out.println("\n  避免死锁的最佳实践：");
        System.out.println("  1. 按固定顺序访问表和行（如按 id 升序）");
        System.out.println("  2. 事务尽量短小，减少持锁时间");
        System.out.println("  3. 合理使用索引，避免行锁升级为表锁");
        System.out.println("  4. 使用 SELECT ... FOR UPDATE NOWAIT 快速失败");
        System.out.println();
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) throws InterruptedException {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  MySQL 锁机制演示 — InnoDB 行锁/间隙锁/临键锁（纯内存）║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        demoRecordLock();
        demoGapAndNextKeyLock();
        demoTableVsRowLock();
        demoDeadlock();
    }
}
