package com.example.patterns.structural;

/**
 * 适配器模式演示 — 类适配器 + 对象适配器
 * <p>
 * 业务场景：第三方日志系统适配
 * - 系统使用统一的 Logger 接口
 * - 需要适配第三方的 ThirdPartyLogger（接口不兼容）
 * - 类适配器：通过继承适配
 * - 对象适配器：通过组合适配（推荐）
 * </p>
 */
public class AdapterDemo {

    public static void main(String[] args) {
        System.out.println("========== 适配器模式演示 ==========\n");

        demonstrateObjectAdapter();
        System.out.println();
        demonstrateClassAdapter();
        System.out.println();
        demonstrateRealWorldExample();
    }

    // ==================== 目标接口 ====================

    /** 系统统一的日志接口 */
    interface Logger {
        void info(String message);
        void error(String message);
    }

    // ==================== 被适配者 ====================

    /** 第三方日志库（接口不兼容） */
    static class ThirdPartyLogger {
        public void writeLog(String level, String msg) {
            System.out.println("    [第三方日志] " + level + ": " + msg);
        }
    }

    // ==================== 1. 对象适配器（推荐） ====================

    /** 对象适配器：通过组合持有被适配者 */
    static class ObjectLoggerAdapter implements Logger {
        private final ThirdPartyLogger adaptee;

        ObjectLoggerAdapter(ThirdPartyLogger adaptee) {
            this.adaptee = adaptee;
        }

        @Override
        public void info(String message) {
            adaptee.writeLog("INFO", message);
        }

        @Override
        public void error(String message) {
            adaptee.writeLog("ERROR", message);
        }
    }

    private static void demonstrateObjectAdapter() {
        System.out.println("--- 1. 对象适配器（组合方式，推荐） ---");
        ThirdPartyLogger thirdParty = new ThirdPartyLogger();
        Logger logger = new ObjectLoggerAdapter(thirdParty);
        logger.info("用户登录成功");
        logger.error("数据库连接失败");
        System.out.println("  优点：更灵活，可以适配多个不同的被适配者");
    }

    // ==================== 2. 类适配器 ====================

    /** 类适配器：通过继承被适配者 */
    static class ClassLoggerAdapter extends ThirdPartyLogger implements Logger {
        @Override
        public void info(String message) {
            writeLog("INFO", message);
        }

        @Override
        public void error(String message) {
            writeLog("ERROR", message);
        }
    }

    private static void demonstrateClassAdapter() {
        System.out.println("--- 2. 类适配器（继承方式） ---");
        Logger logger = new ClassLoggerAdapter();
        logger.info("系统启动完成");
        logger.error("配置文件缺失");
        System.out.println("  缺点：Java 单继承限制，只能适配一个类");
    }

    // ==================== 3. 实际应用示例 ====================

    /**
     * 模拟 InputStreamReader 的适配器角色
     * InputStreamReader 将字节流（InputStream）适配为字符流（Reader）
     */
    interface CharReader {
        String read();
    }

    static class ByteStream {
        private final byte[] data;
        ByteStream(byte[] data) { this.data = data; }
        public byte[] readBytes() { return data; }
    }

    /** 模拟 InputStreamReader：将字节流适配为字符流 */
    static class ByteToCharAdapter implements CharReader {
        private final ByteStream byteStream;
        private final String charset;

        ByteToCharAdapter(ByteStream byteStream, String charset) {
            this.byteStream = byteStream;
            this.charset = charset;
        }

        @Override
        public String read() {
            byte[] bytes = byteStream.readBytes();
            return new String(bytes, java.nio.charset.Charset.forName(charset));
        }
    }

    private static void demonstrateRealWorldExample() {
        System.out.println("--- 3. 实际应用：模拟 InputStreamReader ---");
        ByteStream byteStream = new ByteStream("Hello, 适配器模式!".getBytes());
        CharReader reader = new ByteToCharAdapter(byteStream, "UTF-8");
        System.out.println("    读取结果: " + reader.read());
        System.out.println("  类似 JDK 中 InputStreamReader 将字节流适配为字符流");
    }
}
