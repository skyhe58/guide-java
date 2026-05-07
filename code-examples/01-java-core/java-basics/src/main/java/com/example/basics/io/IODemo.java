package com.example.basics.io;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Stream;

/**
 * IO 流与 NIO 示例
 *
 * 核心知识点：
 * 1. 字节流 vs 字符流
 * 2. 缓冲流的性能优势
 * 3. NIO Channel/Buffer 模型
 * 4. MappedByteBuffer 内存映射文件
 * 5. try-with-resources 资源管理
 * 6. Files/Path API（JDK 7+）
 *
 * 对应文档：docs/java-basics/io-streams.md
 */
public class IODemo {

    public static void main(String[] args) throws Exception {
        Path tempDir = Files.createTempDirectory("io-demo-");
        System.out.println("临时目录: " + tempDir);

        try {
            System.out.println("===== 1. NIO Files API =====");
            demonstrateFilesAPI(tempDir);

            System.out.println("\n===== 2. 字节流 vs 字符流 =====");
            demonstrateStreams(tempDir);

            System.out.println("\n===== 3. 缓冲流性能对比 =====");
            demonstrateBufferedPerformance(tempDir);

            System.out.println("\n===== 4. NIO Channel/Buffer =====");
            demonstrateNIO(tempDir);

            System.out.println("\n===== 5. MappedByteBuffer =====");
            demonstrateMappedByteBuffer(tempDir);

            System.out.println("\n===== 6. 文件遍历 =====");
            demonstrateFileWalk(tempDir);
        } finally {
            // 清理临时文件
            try (Stream<Path> walk = Files.walk(tempDir)) {
                walk.sorted(java.util.Comparator.reverseOrder())
                    .forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
            }
        }
    }

    /**
     * NIO Files API：现代 Java 文件操作的首选方式
     */
    static void demonstrateFilesAPI(Path tempDir) throws IOException {
        Path file = tempDir.resolve("test.txt");

        // 写入文件
        Files.writeString(file, "第一行\n第二行\n第三行", StandardCharsets.UTF_8);
        System.out.println("写入文件: " + file.getFileName());

        // 读取整个文件
        String content = Files.readString(file, StandardCharsets.UTF_8);
        System.out.println("读取内容: " + content.replace("\n", " | "));

        // 逐行读取（大文件友好，惰性加载）
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        System.out.println("行数: " + lines.size());

        // Stream 方式逐行处理
        try (Stream<String> lineStream = Files.lines(file, StandardCharsets.UTF_8)) {
            long nonEmptyLines = lineStream.filter(l -> !l.isEmpty()).count();
            System.out.println("非空行数: " + nonEmptyLines);
        }

        // 文件属性
        System.out.println("文件大小: " + Files.size(file) + " bytes");
        System.out.println("是否存在: " + Files.exists(file));
    }

    /**
     * 字节流与字符流对比
     */
    static void demonstrateStreams(Path tempDir) throws IOException {
        Path file = tempDir.resolve("stream-test.txt");

        // 字符流写入（适合文本）
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write("Hello, 字符流!");
            writer.newLine();
            writer.write("第二行内容");
        }

        // 字符流读取
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("字符流读取: " + line);
            }
        }

        // 字节流写入（适合二进制数据）
        Path binFile = tempDir.resolve("binary.dat");
        try (OutputStream os = new BufferedOutputStream(Files.newOutputStream(binFile))) {
            os.write(new byte[]{0x48, 0x65, 0x6C, 0x6C, 0x6F}); // "Hello"
        }

        // 字节流读取
        byte[] bytes = Files.readAllBytes(binFile);
        System.out.println("字节流读取: " + new String(bytes, StandardCharsets.UTF_8));
    }

    /**
     * 缓冲流 vs 非缓冲流性能对比
     */
    static void demonstrateBufferedPerformance(Path tempDir) throws IOException {
        Path file = tempDir.resolve("perf-test.dat");
        int dataSize = 100_000;

        // 准备测试数据
        byte[] data = new byte[dataSize];
        for (int i = 0; i < dataSize; i++) data[i] = (byte) (i % 256);

        // 非缓冲写入
        long start1 = System.nanoTime();
        try (FileOutputStream fos = new FileOutputStream(file.toFile())) {
            for (byte b : data) {
                fos.write(b); // 每次写 1 字节
            }
        }
        long time1 = System.nanoTime() - start1;

        // 缓冲写入
        long start2 = System.nanoTime();
        try (BufferedOutputStream bos = new BufferedOutputStream(
                new FileOutputStream(file.toFile()))) {
            for (byte b : data) {
                bos.write(b); // 写入缓冲区，满了才刷盘
            }
        }
        long time2 = System.nanoTime() - start2;

        // 批量写入
        long start3 = System.nanoTime();
        try (FileOutputStream fos = new FileOutputStream(file.toFile())) {
            fos.write(data); // 一次性写入
        }
        long time3 = System.nanoTime() - start3;

        System.out.println("非缓冲逐字节写入: " + time1 / 1_000_000 + " ms");
        System.out.println("缓冲流逐字节写入: " + time2 / 1_000_000 + " ms");
        System.out.println("批量写入:         " + time3 / 1_000_000 + " ms");
    }

    /**
     * NIO Channel + Buffer 模型
     * Buffer 核心概念：position、limit、capacity
     * flip()：切换为读模式（limit=position, position=0）
     * clear()：切换为写模式（position=0, limit=capacity）
     */
    static void demonstrateNIO(Path tempDir) throws IOException {
        Path file = tempDir.resolve("nio-test.txt");
        Files.writeString(file, "Hello NIO Channel!", StandardCharsets.UTF_8);

        // FileChannel 读取
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
            ByteBuffer buffer = ByteBuffer.allocate(1024);

            System.out.println("Buffer 初始状态: position=" + buffer.position()
                    + ", limit=" + buffer.limit() + ", capacity=" + buffer.capacity());

            int bytesRead = channel.read(buffer); // 写入 buffer
            System.out.println("读取 " + bytesRead + " 字节");
            System.out.println("写入后: position=" + buffer.position() + ", limit=" + buffer.limit());

            buffer.flip(); // 切换为读模式
            System.out.println("flip 后: position=" + buffer.position() + ", limit=" + buffer.limit());

            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            System.out.println("内容: " + new String(bytes, StandardCharsets.UTF_8));

            buffer.clear(); // 重置为写模式
            System.out.println("clear 后: position=" + buffer.position() + ", limit=" + buffer.limit());
        }
    }

    /**
     * MappedByteBuffer：内存映射文件
     * 将文件直接映射到内存，通过操作内存来读写文件
     * 适合大文件的随机访问
     */
    static void demonstrateMappedByteBuffer(Path tempDir) throws IOException {
        Path file = tempDir.resolve("mapped-test.dat");

        // 写入测试数据
        byte[] data = "Hello MappedByteBuffer! 这是内存映射文件示例。".getBytes(StandardCharsets.UTF_8);
        Files.write(file, data);

        // 内存映射读取
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
            MappedByteBuffer mappedBuffer = channel.map(
                    FileChannel.MapMode.READ_ONLY, 0, channel.size());

            System.out.println("映射大小: " + mappedBuffer.capacity() + " bytes");
            System.out.println("是否直接缓冲区: " + mappedBuffer.isDirect()); // true

            byte[] readData = new byte[mappedBuffer.remaining()];
            mappedBuffer.get(readData);
            System.out.println("内容: " + new String(readData, StandardCharsets.UTF_8));
        }

        System.out.println("\n大文件读写方案建议:");
        System.out.println("  小文件 (< 几十MB): Files.readAllBytes() / Files.readString()");
        System.out.println("  大文本逐行处理:    Files.lines() (惰性加载)");
        System.out.println("  大文件随机访问:    MappedByteBuffer (零拷贝)");
        System.out.println("  中大文件顺序读写:  FileChannel + ByteBuffer");
    }

    /**
     * 文件遍历
     */
    static void demonstrateFileWalk(Path tempDir) throws IOException {
        // 创建一些测试文件
        Path subDir = tempDir.resolve("subdir");
        Files.createDirectories(subDir);
        Files.writeString(tempDir.resolve("a.txt"), "file a");
        Files.writeString(tempDir.resolve("b.java"), "file b");
        Files.writeString(subDir.resolve("c.txt"), "file c");

        // Files.walk：递归遍历
        System.out.println("递归遍历:");
        try (Stream<Path> walk = Files.walk(tempDir, 2)) {
            walk.filter(Files::isRegularFile)
                .forEach(p -> System.out.println("  " + tempDir.relativize(p)));
        }

        // Files.find：带条件查找
        System.out.println("查找 .txt 文件:");
        try (Stream<Path> found = Files.find(tempDir, 2,
                (p, attr) -> p.toString().endsWith(".txt") && attr.isRegularFile())) {
            found.forEach(p -> System.out.println("  " + tempDir.relativize(p)));
        }
    }
}
