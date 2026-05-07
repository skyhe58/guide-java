package com.example.advanced.network;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

/**
 * 网络编程演示 —— BIO/NIO 对比
 * <p>
 * 演示内容：
 * 1. BIO 服务器（同步阻塞，一连接一线程）
 * 2. NIO 服务器（Selector 多路复用）
 * 3. Buffer 核心操作
 * </p>
 *
 * 注意：Netty 示例需要额外依赖，这里只演示 JDK 原生 BIO/NIO
 */
public class NIODemo {

    public static void main(String[] args) throws Exception {
        System.out.println("========== BIO/NIO 网络编程演示 ==========\n");

        demonstrateBuffer();
        System.out.println();

        demonstrateBIOServer();
        System.out.println();

        demonstrateNIOServer();
    }

    // ==================== 1. Buffer 操作 ====================

    /**
     * NIO Buffer 的核心操作演示
     */
    private static void demonstrateBuffer() {
        System.out.println("--- 1. NIO Buffer 操作 ---");

        // 创建 Buffer
        ByteBuffer buffer = ByteBuffer.allocate(10);
        System.out.println("初始状态:");
        printBufferState(buffer);

        // 写入数据
        buffer.put((byte) 'H');
        buffer.put((byte) 'i');
        buffer.put((byte) '!');
        System.out.println("\n写入 'Hi!' 后:");
        printBufferState(buffer);

        // flip：切换到读模式
        buffer.flip();
        System.out.println("\nflip() 后（切换到读模式）:");
        printBufferState(buffer);

        // 读取数据
        byte b1 = buffer.get();
        byte b2 = buffer.get();
        System.out.println("\n读取 2 个字节: " + (char) b1 + (char) b2);
        printBufferState(buffer);

        // compact：压缩（保留未读数据，切换到写模式）
        buffer.compact();
        System.out.println("\ncompact() 后（保留未读数据，切换到写模式）:");
        printBufferState(buffer);

        // clear：清空（切换到写模式）
        buffer.clear();
        System.out.println("\nclear() 后:");
        printBufferState(buffer);

        // Direct Buffer vs Heap Buffer
        System.out.println("\n--- Direct Buffer vs Heap Buffer ---");
        ByteBuffer heapBuffer = ByteBuffer.allocate(1024);
        ByteBuffer directBuffer = ByteBuffer.allocateDirect(1024);
        System.out.println("Heap Buffer:  " + heapBuffer.getClass().getSimpleName()
                + ", isDirect=" + heapBuffer.isDirect());
        System.out.println("Direct Buffer: " + directBuffer.getClass().getSimpleName()
                + ", isDirect=" + directBuffer.isDirect());
        System.out.println("Direct Buffer 分配在堆外内存，减少一次拷贝，适合大量 IO 操作");
    }

    private static void printBufferState(ByteBuffer buffer) {
        System.out.println("  position=" + buffer.position()
                + ", limit=" + buffer.limit()
                + ", capacity=" + buffer.capacity()
                + ", remaining=" + buffer.remaining());
    }

    // ==================== 2. BIO 服务器 ====================

    /**
     * BIO 服务器演示
     * 每个连接一个线程，accept 和 read 都会阻塞
     */
    private static void demonstrateBIOServer() throws Exception {
        System.out.println("--- 2. BIO 服务器演示 ---");

        int port = 9001;
        // 启动 BIO 服务器
        Thread serverThread = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                serverSocket.setSoTimeout(2000); // 2 秒超时，避免永久阻塞
                System.out.println("  [BIO Server] 启动在端口 " + port);

                try {
                    Socket socket = serverSocket.accept(); // 阻塞等待连接
                    // 每个连接一个线程处理
                    try (InputStream in = socket.getInputStream();
                         OutputStream out = socket.getOutputStream()) {
                        byte[] buffer = new byte[1024];
                        int len = in.read(buffer); // 阻塞等待数据
                        if (len > 0) {
                            String request = new String(buffer, 0, len);
                            System.out.println("  [BIO Server] 收到: " + request);
                            out.write(("BIO Echo: " + request).getBytes());
                        }
                    }
                    socket.close();
                } catch (SocketTimeoutException e) {
                    System.out.println("  [BIO Server] 等待连接超时");
                }
            } catch (IOException e) {
                System.out.println("  [BIO Server] 错误: " + e.getMessage());
            }
        }, "BIO-Server");
        serverThread.start();

        Thread.sleep(200); // 等待服务器启动

        // 客户端连接
        try (Socket client = new Socket("localhost", port)) {
            client.getOutputStream().write("Hello BIO!".getBytes());
            client.getOutputStream().flush();

            byte[] buffer = new byte[1024];
            int len = client.getInputStream().read(buffer);
            if (len > 0) {
                System.out.println("  [BIO Client] 收到: " + new String(buffer, 0, len));
            }
        }

        serverThread.join(3000);
        System.out.println("  [BIO] 问题：每个连接需要一个线程，高并发时线程数暴增");
    }

    // ==================== 3. NIO 服务器 ====================

    /**
     * NIO 服务器演示
     * 使用 Selector 多路复用，一个线程管理多个连接
     */
    private static void demonstrateNIOServer() throws Exception {
        System.out.println("--- 3. NIO 服务器演示（Selector 多路复用） ---");

        int port = 9002;

        // 启动 NIO 服务器
        Thread serverThread = new Thread(() -> {
            try {
                Selector selector = Selector.open();
                ServerSocketChannel serverChannel = ServerSocketChannel.open();
                serverChannel.bind(new InetSocketAddress(port));
                serverChannel.configureBlocking(false); // 非阻塞模式
                serverChannel.register(selector, SelectionKey.OP_ACCEPT);

                System.out.println("  [NIO Server] 启动在端口 " + port);

                long startTime = System.currentTimeMillis();
                while (System.currentTimeMillis() - startTime < 3000) { // 3 秒后退出
                    int readyCount = selector.select(1000); // 1 秒超时
                    if (readyCount == 0) continue;

                    Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                    while (keys.hasNext()) {
                        SelectionKey key = keys.next();
                        keys.remove();

                        if (key.isAcceptable()) {
                            // 处理新连接
                            SocketChannel client = serverChannel.accept();
                            if (client != null) {
                                client.configureBlocking(false);
                                client.register(selector, SelectionKey.OP_READ);
                                System.out.println("  [NIO Server] 新连接: "
                                        + client.getRemoteAddress());
                            }
                        } else if (key.isReadable()) {
                            // 处理读事件
                            SocketChannel client = (SocketChannel) key.channel();
                            ByteBuffer buffer = ByteBuffer.allocate(1024);
                            int len = client.read(buffer);
                            if (len > 0) {
                                buffer.flip();
                                byte[] data = new byte[buffer.remaining()];
                                buffer.get(data);
                                String request = new String(data);
                                System.out.println("  [NIO Server] 收到: " + request);

                                // 回写响应
                                ByteBuffer response = ByteBuffer.wrap(
                                        ("NIO Echo: " + request).getBytes());
                                client.write(response);
                            } else if (len == -1) {
                                key.cancel();
                                client.close();
                            }
                        }
                    }
                }
                serverChannel.close();
                selector.close();
            } catch (IOException e) {
                System.out.println("  [NIO Server] 错误: " + e.getMessage());
            }
        }, "NIO-Server");
        serverThread.start();

        Thread.sleep(200); // 等待服务器启动

        // 多个客户端连接（NIO 一个线程处理所有连接）
        for (int i = 1; i <= 3; i++) {
            final int clientId = i;
            try (SocketChannel client = SocketChannel.open(
                    new InetSocketAddress("localhost", port))) {
                ByteBuffer writeBuffer = ByteBuffer.wrap(
                        ("Hello NIO from client " + clientId).getBytes());
                client.write(writeBuffer);

                ByteBuffer readBuffer = ByteBuffer.allocate(1024);
                client.read(readBuffer);
                readBuffer.flip();
                byte[] data = new byte[readBuffer.remaining()];
                readBuffer.get(data);
                System.out.println("  [NIO Client " + clientId + "] 收到: " + new String(data));
            }
        }

        serverThread.join(5000);
        System.out.println("\n  [NIO] 优势：一个线程通过 Selector 管理多个连接，高并发下性能优异");
        System.out.println("  [NIO] Selector 底层：Linux 使用 epoll，macOS 使用 kqueue");
    }
}
