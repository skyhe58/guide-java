package com.example.patterns.behavioral;

/**
 * 模板方法模式演示 — 模拟 AQS 的模板方法设计
 * <p>
 * 业务场景：模拟 AbstractQueuedSynchronizer (AQS) 的锁获取流程
 * - 模板方法 acquire() 定义了获取锁的算法骨架
 * - tryAcquire() 由子类实现具体的获取逻辑
 * - 公平锁和非公平锁通过不同的 tryAcquire 实现
 * </p>
 */
public class TemplateMethodDemo {

    public static void main(String[] args) {
        System.out.println("========== 模板方法模式演示 ==========\n");

        demonstrateSimpleLock();
        System.out.println();
        demonstrateDataProcessor();
    }

    // ==================== 1. 模拟 AQS 模板方法 ====================

    /**
     * 模拟 AQS 的简化版本
     * acquire() 是模板方法，定义了获取锁的骨架
     * tryAcquire() 是抽象方法，由子类实现
     */
    static abstract class SimpleSync {
        private volatile int state = 0;
        private Thread owner = null;

        /** 模板方法：定义获取锁的算法骨架（类似 AQS.acquire） */
        public final void acquire() {
            if (!tryAcquire()) {          // 子类实现
                System.out.println("    获取锁失败，进入等待队列...");
                enqueue();                 // 通用逻辑：加入等待队列
                parkAndRetry();            // 通用逻辑：阻塞并重试
            }
            System.out.println("    " + Thread.currentThread().getName() + " 获取锁成功");
        }

        /** 模板方法：定义释放锁的算法骨架（类似 AQS.release） */
        public final void release() {
            if (tryRelease()) {           // 子类实现
                System.out.println("    " + Thread.currentThread().getName() + " 释放锁成功");
                unparkNext();              // 通用逻辑：唤醒下一个等待线程
            }
        }

        /** 抽象方法：子类实现获取锁的具体逻辑 */
        protected abstract boolean tryAcquire();

        /** 抽象方法：子类实现释放锁的具体逻辑 */
        protected abstract boolean tryRelease();

        // 通用逻辑（AQS 中的 CLH 队列操作）
        private void enqueue() {
            System.out.println("    [AQS通用] 将当前线程加入 CLH 等待队列");
        }

        private void parkAndRetry() {
            System.out.println("    [AQS通用] 阻塞当前线程，等待唤醒后重试");
        }

        private void unparkNext() {
            System.out.println("    [AQS通用] 唤醒等待队列中的下一个线程");
        }

        protected int getState() { return state; }
        protected void setState(int state) { this.state = state; }
        protected Thread getOwner() { return owner; }
        protected void setOwner(Thread owner) { this.owner = owner; }
    }

    /** 非公平锁实现（类似 ReentrantLock.NonfairSync） */
    static class NonfairSync extends SimpleSync {
        @Override
        protected boolean tryAcquire() {
            System.out.println("    [非公平锁] 直接尝试 CAS 获取锁（不检查队列）");
            if (getState() == 0) {
                setState(1);
                setOwner(Thread.currentThread());
                return true;
            }
            return false;
        }

        @Override
        protected boolean tryRelease() {
            setState(0);
            setOwner(null);
            return true;
        }
    }

    /** 公平锁实现（类似 ReentrantLock.FairSync） */
    static class FairSync extends SimpleSync {
        @Override
        protected boolean tryAcquire() {
            System.out.println("    [公平锁] 先检查等待队列，再尝试获取锁");
            if (getState() == 0) {
                // 公平锁：先检查是否有线程在排队
                System.out.println("    [公平锁] 检查队列中是否有等待线程...");
                setState(1);
                setOwner(Thread.currentThread());
                return true;
            }
            return false;
        }

        @Override
        protected boolean tryRelease() {
            setState(0);
            setOwner(null);
            return true;
        }
    }

    private static void demonstrateSimpleLock() {
        System.out.println("--- 1. 模拟 AQS 模板方法 ---");

        System.out.println("\n非公平锁：");
        SimpleSync nonfair = new NonfairSync();
        nonfair.acquire();
        nonfair.release();

        System.out.println("\n公平锁：");
        SimpleSync fair = new FairSync();
        fair.acquire();
        fair.release();

        System.out.println("\n  模板方法的核心：");
        System.out.println("  acquire()/release() 定义了算法骨架（final 不可重写）");
        System.out.println("  tryAcquire()/tryRelease() 由子类实现具体逻辑");
    }

    // ==================== 2. 数据处理模板 ====================

    /** 数据处理模板（类似 JdbcTemplate 的设计思想） */
    static abstract class DataProcessor {
        /** 模板方法：定义数据处理流程 */
        public final void process() {
            openConnection();
            String data = readData();       // 子类实现
            String result = processData(data); // 子类实现
            writeResult(result);            // 子类实现
            closeConnection();
        }

        // 通用步骤
        private void openConnection() {
            System.out.println("    [通用] 打开连接");
        }
        private void closeConnection() {
            System.out.println("    [通用] 关闭连接");
        }

        // 子类实现的步骤
        protected abstract String readData();
        protected abstract String processData(String data);
        protected abstract void writeResult(String result);
    }

    /** CSV 数据处理器 */
    static class CsvProcessor extends DataProcessor {
        @Override
        protected String readData() {
            System.out.println("    [CSV] 读取 CSV 文件");
            return "name,age\n张三,25";
        }

        @Override
        protected String processData(String data) {
            System.out.println("    [CSV] 解析 CSV 数据");
            return data.replace(",", " | ");
        }

        @Override
        protected void writeResult(String result) {
            System.out.println("    [CSV] 输出结果: " + result);
        }
    }

    /** JSON 数据处理器 */
    static class JsonProcessor extends DataProcessor {
        @Override
        protected String readData() {
            System.out.println("    [JSON] 读取 JSON 数据");
            return "{\"name\":\"李四\",\"age\":30}";
        }

        @Override
        protected String processData(String data) {
            System.out.println("    [JSON] 解析 JSON 数据");
            return data.replace("\"", "").replace("{", "").replace("}", "");
        }

        @Override
        protected void writeResult(String result) {
            System.out.println("    [JSON] 输出结果: " + result);
        }
    }

    private static void demonstrateDataProcessor() {
        System.out.println("--- 2. 数据处理模板（类似 JdbcTemplate） ---");

        System.out.println("\nCSV 处理：");
        new CsvProcessor().process();

        System.out.println("\nJSON 处理：");
        new JsonProcessor().process();
    }
}
