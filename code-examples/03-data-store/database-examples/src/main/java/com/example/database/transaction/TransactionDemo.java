package com.example.database.transaction;

/**
 * MySQL 事务与隔离级别演示 — 用多线程+锁模拟四种隔离级别的并发行为
 *
 * <p>本示例用 Java 并发工具模拟 MySQL InnoDB 的事务隔离级别：
 * <ul>
 *   <li>READ UNCOMMITTED（读未提交）— 可能脏读</li>
 *   <li>READ COMMITTED（读已提交）— 解决脏读，可能不可重复读</li>
 *   <li>REPEATABLE READ（可重复读）— MySQL 默认级别，MVCC 实现</li>
 *   <li>SERIALIZABLE（串行化）— 最高隔离，完全串行执行</li>
 * </ul>
 *
 * <p>同时演示 MVCC（多版本并发控制）的核心机制：每行数据维护版本链，
 * 事务根据自己的 ReadView 决定能看到哪个版本。</p>
 *
 * <p>如需连接真实 MySQL，使用：
 * {@code docker compose -f docker/docker-compose.yml up -d mysql}</p>
 *
 * <h3>MVCC 版本链示意：</h3>
 * <pre>
 *  当前行 → [trx_id=3, value=300] → [trx_id=2, value=200] → [trx_id=1, value=100]
 *
 *  ReadView 包含：
 *  - creator_trx_id：创建 ReadView 的事务 ID
 *  - min_trx_id：活跃事务列表中最小的事务 ID
 *  - max_trx_id：下一个将分配的事务 ID
 *  - active_trx_ids：创建 ReadView 时所有活跃（未提交）的事务 ID 列表
 * </pre>
 */
public class TransactionDemo {

    // ==================== MVCC 版本链 ====================

    /** 行数据的一个版本，对应 InnoDB 中 undo log 中的历史版本 */
    static class RowVersion {
        final long trxId;           // 创建该版本的事务 ID
        final int value;            // 数据值
        final RowVersion previous;  // 指向上一个版本（undo log 链）

        RowVersion(long trxId, int value, RowVersion previous) {
            this.trxId = trxId;
            this.value = value;
            this.previous = previous;
        }
    }

    /** ReadView：事务的一致性视图，决定该事务能看到哪些版本 */
    static class ReadView {
        final long creatorTrxId;                        // 创建该 ReadView 的事务 ID
        final long minTrxId;                            // 活跃事务中最小的 ID
        final long maxTrxId;                            // 下一个将分配的事务 ID（当前最大 + 1）
        final java.util.Set<Long> activeTrxIds;        // 创建时所有活跃事务的 ID 集合

        ReadView(long creatorTrxId, long minTrxId, long maxTrxId, java.util.Set<Long> activeTrxIds) {
            this.creatorTrxId = creatorTrxId;
            this.minTrxId = minTrxId;
            this.maxTrxId = maxTrxId;
            this.activeTrxIds = activeTrxIds;
        }

        /**
         * 判断某个版本对当前 ReadView 是否可见。
         * InnoDB 的可见性判断规则：
         * 1. 如果版本的 trxId == creatorTrxId → 可见（自己修改的）
         * 2. 如果版本的 trxId < minTrxId → 可见（在 ReadView 创建前已提交）
         * 3. 如果版本的 trxId >= maxTrxId → 不可见（在 ReadView 创建后才开始）
         * 4. 如果 minTrxId <= trxId < maxTrxId：
         *    - trxId 在 activeTrxIds 中 → 不可见（该事务还未提交）
         *    - trxId 不在 activeTrxIds 中 → 可见（该事务已提交）
         */
        boolean isVisible(long trxId) {
            if (trxId == creatorTrxId) return true;
            if (trxId < minTrxId) return true;
            if (trxId >= maxTrxId) return false;
            return !activeTrxIds.contains(trxId);
        }
    }

    /** 模拟一行数据，维护版本链 */
    static class MvccRow {
        private volatile RowVersion latest;
        private final java.util.concurrent.locks.ReentrantLock writeLock =
                new java.util.concurrent.locks.ReentrantLock();

        MvccRow(long trxId, int initialValue) {
            this.latest = new RowVersion(trxId, initialValue, null);
        }

        /** 写入新版本（加排他锁） */
        void update(long trxId, int newValue) {
            writeLock.lock();
            try {
                latest = new RowVersion(trxId, newValue, latest);
            } finally {
                writeLock.unlock();
            }
        }

        /** 根据 ReadView 读取可见版本（沿版本链回溯） */
        int readWithMvcc(ReadView readView) {
            RowVersion version = latest;
            while (version != null) {
                if (readView.isVisible(version.trxId)) {
                    return version.value;
                }
                version = version.previous;
            }
            throw new IllegalStateException("没有可见版本（不应该发生）");
        }

        /** 直接读取最新版本（不走 MVCC，模拟 READ UNCOMMITTED） */
        int readLatest() {
            return latest.value;
        }

        /** 获取版本链信息 */
        String getVersionChain() {
            StringBuilder sb = new StringBuilder();
            RowVersion v = latest;
            while (v != null) {
                if (sb.length() > 0) sb.append(" → ");
                sb.append(String.format("[trx=%d, val=%d]", v.trxId, v.value));
                v = v.previous;
            }
            return sb.toString();
        }
    }

    // ==================== 事务管理器 ====================

    /** 简化的事务管理器，负责分配事务 ID 和管理活跃事务 */
    static class TransactionManager {
        private final java.util.concurrent.atomic.AtomicLong nextTrxId =
                new java.util.concurrent.atomic.AtomicLong(1);
        private final java.util.concurrent.ConcurrentSkipListSet<Long> activeTrxIds =
                new java.util.concurrent.ConcurrentSkipListSet<>();

        long beginTransaction() {
            long trxId = nextTrxId.getAndIncrement();
            activeTrxIds.add(trxId);
            return trxId;
        }

        void commit(long trxId) {
            activeTrxIds.remove(trxId);
        }

        /** 创建 ReadView（快照） */
        ReadView createReadView(long creatorTrxId) {
            java.util.Set<Long> snapshot = new java.util.HashSet<>(activeTrxIds);
            long min = snapshot.isEmpty() ? nextTrxId.get() : snapshot.iterator().next();
            long max = nextTrxId.get();
            return new ReadView(creatorTrxId, min, max, snapshot);
        }
    }

    // ==================== 演示方法 ====================

    /**
     * 演示1：READ UNCOMMITTED — 脏读问题
     * 事务 A 修改数据但未提交，事务 B 能读到未提交的数据（脏数据）
     */
    static void demoReadUncommitted() throws InterruptedException {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示1：READ UNCOMMITTED — 脏读");
        System.out.println("═══════════════════════════════════════════════════");

        TransactionManager tm = new TransactionManager();
        long initTrx = tm.beginTransaction();
        MvccRow row = new MvccRow(initTrx, 100);
        tm.commit(initTrx);

        java.util.concurrent.CyclicBarrier barrier = new java.util.concurrent.CyclicBarrier(2);
        java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(2);

        // 事务 A：修改数据但不立即提交
        new Thread(() -> {
            try {
                long trxA = tm.beginTransaction();
                System.out.println("\n  [事务A] 开始，修改 value: 100 → 200（未提交）");
                row.update(trxA, 200);
                barrier.await(); // 等事务 B 读取
                Thread.sleep(100);
                // 模拟事务 A 回滚（不提交）
                System.out.println("  [事务A] 回滚！数据应该恢复为 100");
                row.update(trxA, 100);
                tm.commit(trxA);
                done.countDown();
            } catch (Exception e) { e.printStackTrace(); }
        }).start();

        // 事务 B：READ UNCOMMITTED 级别，直接读最新值
        new Thread(() -> {
            try {
                barrier.await();
                long trxB = tm.beginTransaction();
                // READ UNCOMMITTED：不使用 MVCC，直接读最新版本
                int dirtyValue = row.readLatest();
                System.out.printf("  [事务B] READ UNCOMMITTED 读到: %d（脏读！事务A还未提交）%n", dirtyValue);
                tm.commit(trxB);
                done.countDown();
            } catch (Exception e) { e.printStackTrace(); }
        }).start();

        done.await();
        System.out.println("  结论：READ UNCOMMITTED 会读到其他事务未提交的数据，如果对方回滚就是脏数据");
        System.out.println();
    }

    /**
     * 演示2：READ COMMITTED — 解决脏读，但存在不可重复读
     * 每次 SELECT 都创建新的 ReadView，所以同一事务内两次读可能结果不同
     */
    static void demoReadCommitted() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示2：READ COMMITTED — 不可重复读");
        System.out.println("═══════════════════════════════════════════════════");

        TransactionManager tm = new TransactionManager();
        long initTrx = tm.beginTransaction();
        MvccRow row = new MvccRow(initTrx, 100);
        tm.commit(initTrx);

        // 事务 B 开始
        long trxB = tm.beginTransaction();

        // 事务 B 第一次读：创建 ReadView
        ReadView rv1 = tm.createReadView(trxB);
        int read1 = row.readWithMvcc(rv1);
        System.out.printf("\n  [事务B] 第一次读: %d%n", read1);

        // 事务 A 修改并提交
        long trxA = tm.beginTransaction();
        row.update(trxA, 200);
        tm.commit(trxA);
        System.out.println("  [事务A] 修改 value: 100 → 200 并提交");

        // 事务 B 第二次读：READ COMMITTED 每次 SELECT 创建新的 ReadView
        ReadView rv2 = tm.createReadView(trxB);
        int read2 = row.readWithMvcc(rv2);
        System.out.printf("  [事务B] 第二次读: %d（新 ReadView 能看到事务A已提交的数据）%n", read2);

        tm.commit(trxB);
        System.out.printf("  结论：同一事务内两次读结果不同（%d → %d），这就是不可重复读%n", read1, read2);
        System.out.println("  版本链: " + row.getVersionChain());
        System.out.println();
    }

    /**
     * 演示3：REPEATABLE READ — MySQL 默认级别，MVCC 保证可重复读
     * 事务开始时创建一次 ReadView，后续所有读都使用同一个 ReadView
     */
    static void demoRepeatableRead() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示3：REPEATABLE READ — 可重复读（MySQL 默认）");
        System.out.println("═══════════════════════════════════════════════════");

        TransactionManager tm = new TransactionManager();
        long initTrx = tm.beginTransaction();
        MvccRow row = new MvccRow(initTrx, 100);
        tm.commit(initTrx);

        // 事务 B 开始，创建一次 ReadView（REPEATABLE READ 只在第一次 SELECT 时创建）
        long trxB = tm.beginTransaction();
        ReadView rv = tm.createReadView(trxB);

        int read1 = row.readWithMvcc(rv);
        System.out.printf("\n  [事务B] 第一次读: %d%n", read1);

        // 事务 A 修改并提交
        long trxA = tm.beginTransaction();
        row.update(trxA, 200);
        tm.commit(trxA);
        System.out.println("  [事务A] 修改 value: 100 → 200 并提交");

        // 事务 C 修改并提交
        long trxC = tm.beginTransaction();
        row.update(trxC, 300);
        tm.commit(trxC);
        System.out.println("  [事务C] 修改 value: 200 → 300 并提交");

        // 事务 B 第二次读：使用同一个 ReadView，看不到事务 A 和 C 的修改
        int read2 = row.readWithMvcc(rv);
        System.out.printf("  [事务B] 第二次读: %d（使用同一个 ReadView，结果不变）%n", read2);

        int read3 = row.readWithMvcc(rv);
        System.out.printf("  [事务B] 第三次读: %d（始终一致）%n", read3);

        tm.commit(trxB);
        System.out.printf("  结论：三次读结果一致（%d = %d = %d），MVCC 保证了可重复读%n", read1, read2, read3);
        System.out.println("  版本链: " + row.getVersionChain());
        System.out.println();
    }

    /**
     * 演示4：SERIALIZABLE — 串行化执行
     * 所有读操作加共享锁，写操作加排他锁，完全串行
     */
    static void demoSerializable() throws InterruptedException {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示4：SERIALIZABLE — 串行化");
        System.out.println("═══════════════════════════════════════════════════");

        // 用 ReentrantReadWriteLock 模拟 SERIALIZABLE 的锁行为
        java.util.concurrent.locks.ReentrantReadWriteLock rwLock =
                new java.util.concurrent.locks.ReentrantReadWriteLock();
        final int[] value = {100};
        java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(2);

        System.out.println("\n  SERIALIZABLE 级别：读加共享锁，写加排他锁");

        // 事务 A：持有读锁
        new Thread(() -> {
            try {
                rwLock.readLock().lock();
                System.out.println("  [事务A] 获取读锁，读取 value = " + value[0]);
                Thread.sleep(200);
                System.out.println("  [事务A] 释放读锁");
                rwLock.readLock().unlock();
                done.countDown();
            } catch (Exception e) { e.printStackTrace(); }
        }).start();

        Thread.sleep(50);

        // 事务 B：尝试获取写锁（会被阻塞直到事务 A 释放读锁）
        new Thread(() -> {
            try {
                System.out.println("  [事务B] 尝试获取写锁（被事务A的读锁阻塞）...");
                long start = System.currentTimeMillis();
                rwLock.writeLock().lock();
                long waited = System.currentTimeMillis() - start;
                value[0] = 200;
                System.out.printf("  [事务B] 获取写锁成功（等待了 %d ms），修改 value = %d%n", waited, value[0]);
                rwLock.writeLock().unlock();
                done.countDown();
            } catch (Exception e) { e.printStackTrace(); }
        }).start();

        done.await();
        System.out.println("  结论：SERIALIZABLE 通过锁机制保证完全串行，性能最差但隔离性最强");
        System.out.println();
    }

    /** 演示5：四种隔离级别对比总结 */
    static void demoIsolationComparison() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示5：四种隔离级别对比");
        System.out.println("═══════════════════════════════════════════════════");

        String[][] comparison = {
                {"隔离级别",           "脏读", "不可重复读", "幻读",   "实现方式",           "性能"},
                {"READ UNCOMMITTED",  "✓",   "✓",        "✓",     "无锁/无MVCC",        "最高"},
                {"READ COMMITTED",    "✗",   "✓",        "✓",     "MVCC(每次新ReadView)", "高"},
                {"REPEATABLE READ",   "✗",   "✗",        "✗*",    "MVCC(复用ReadView)",   "中"},
                {"SERIALIZABLE",      "✗",   "✗",        "✗",     "读写锁串行",           "最低"},
        };

        System.out.println();
        for (int i = 0; i < comparison.length; i++) {
            System.out.printf("  %-22s %-6s %-10s %-6s %-22s %-6s%n",
                    comparison[i][0], comparison[i][1], comparison[i][2],
                    comparison[i][3], comparison[i][4], comparison[i][5]);
            if (i == 0) {
                System.out.println("  " + "─".repeat(76));
            }
        }
        System.out.println("\n  * MySQL 的 REPEATABLE READ 通过 Next-Key Lock 在很大程度上避免了幻读");
        System.out.println("  * 但在特定场景下（如先快照读再当前读）仍可能出现幻读");
        System.out.println();
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) throws InterruptedException {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  MySQL 事务隔离级别演示 — MVCC 多版本并发控制（纯内存）  ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        demoReadUncommitted();
        demoReadCommitted();
        demoRepeatableRead();
        demoSerializable();
        demoIsolationComparison();
    }
}
