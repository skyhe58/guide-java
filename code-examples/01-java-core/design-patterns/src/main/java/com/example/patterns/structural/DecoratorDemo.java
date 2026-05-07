package com.example.patterns.structural;

/**
 * 装饰器模式演示 — 模拟 Java IO 流的装饰器设计
 * <p>
 * 业务场景：模拟 InputStream 的装饰器链
 * - DataSource 是 Component（类似 InputStream）
 * - FileDataSource 是 ConcreteComponent（类似 FileInputStream）
 * - DataSourceDecorator 是 Decorator（类似 FilterInputStream）
 * - EncryptionDecorator / CompressionDecorator 是 ConcreteDecorator
 * </p>
 */
public class DecoratorDemo {

    public static void main(String[] args) {
        System.out.println("========== 装饰器模式演示 ==========\n");

        // 1. 基础数据源
        System.out.println("--- 1. 基础数据源（无装饰） ---");
        DataSource fileSource = new FileDataSource("config.yml");
        fileSource.writeData("username=admin");
        System.out.println("读取: " + fileSource.readData());

        // 2. 加密装饰器
        System.out.println("\n--- 2. 加密装饰器 ---");
        DataSource encrypted = new EncryptionDecorator(new FileDataSource("secret.dat"));
        encrypted.writeData("password=123456");
        System.out.println("读取: " + encrypted.readData());

        // 3. 压缩装饰器
        System.out.println("\n--- 3. 压缩装饰器 ---");
        DataSource compressed = new CompressionDecorator(new FileDataSource("data.gz"));
        compressed.writeData("large data content here");
        System.out.println("读取: " + compressed.readData());

        // 4. 装饰器组合（类似 Java IO 的层层包装）
        System.out.println("\n--- 4. 装饰器组合（加密 + 压缩） ---");
        DataSource combined = new EncryptionDecorator(
                new CompressionDecorator(
                        new FileDataSource("archive.dat.enc")
                )
        );
        combined.writeData("sensitive data");
        System.out.println("读取: " + combined.readData());

        System.out.println("\n--- Java IO 中的装饰器模式 ---");
        System.out.println("new BufferedInputStream(new DataInputStream(new FileInputStream(\"file\")))");
        System.out.println("每一层装饰器都增加了新功能，但接口保持一致");
    }

    // ==================== Component 接口 ====================

    /** 数据源接口（类似 InputStream） */
    interface DataSource {
        void writeData(String data);
        String readData();
    }

    // ==================== ConcreteComponent ====================

    /** 文件数据源（类似 FileInputStream） */
    static class FileDataSource implements DataSource {
        private final String filename;
        private String data = "";

        FileDataSource(String filename) {
            this.filename = filename;
        }

        @Override
        public void writeData(String data) {
            this.data = data;
            System.out.println("  [文件] 写入 " + filename + ": " + data);
        }

        @Override
        public String readData() {
            System.out.println("  [文件] 读取 " + filename);
            return data;
        }
    }

    // ==================== Decorator 基类 ====================

    /** 装饰器基类（类似 FilterInputStream） */
    static abstract class DataSourceDecorator implements DataSource {
        protected final DataSource wrappee;

        DataSourceDecorator(DataSource source) {
            this.wrappee = source;
        }

        @Override
        public void writeData(String data) {
            wrappee.writeData(data);
        }

        @Override
        public String readData() {
            return wrappee.readData();
        }
    }

    // ==================== ConcreteDecorator ====================

    /** 加密装饰器 */
    static class EncryptionDecorator extends DataSourceDecorator {
        EncryptionDecorator(DataSource source) {
            super(source);
        }

        @Override
        public void writeData(String data) {
            String encrypted = encrypt(data);
            System.out.println("  [加密] 加密数据: " + data + " → " + encrypted);
            super.writeData(encrypted);
        }

        @Override
        public String readData() {
            String data = super.readData();
            String decrypted = decrypt(data);
            System.out.println("  [加密] 解密数据: " + data + " → " + decrypted);
            return decrypted;
        }

        private String encrypt(String data) {
            // 简单模拟：Base64 风格编码
            StringBuilder sb = new StringBuilder();
            for (char c : data.toCharArray()) {
                sb.append((char) (c + 3));
            }
            return sb.toString();
        }

        private String decrypt(String data) {
            StringBuilder sb = new StringBuilder();
            for (char c : data.toCharArray()) {
                sb.append((char) (c - 3));
            }
            return sb.toString();
        }
    }

    /** 压缩装饰器 */
    static class CompressionDecorator extends DataSourceDecorator {
        CompressionDecorator(DataSource source) {
            super(source);
        }

        @Override
        public void writeData(String data) {
            String compressed = compress(data);
            System.out.println("  [压缩] 压缩数据: " + data.length()
                    + " 字符 → " + compressed.length() + " 字符");
            super.writeData(compressed);
        }

        @Override
        public String readData() {
            String data = super.readData();
            String decompressed = decompress(data);
            System.out.println("  [压缩] 解压数据: " + data.length()
                    + " 字符 → " + decompressed.length() + " 字符");
            return decompressed;
        }

        private String compress(String data) {
            // 简单模拟：标记为已压缩
            return "[Z]" + data;
        }

        private String decompress(String data) {
            return data.startsWith("[Z]") ? data.substring(3) : data;
        }
    }
}
