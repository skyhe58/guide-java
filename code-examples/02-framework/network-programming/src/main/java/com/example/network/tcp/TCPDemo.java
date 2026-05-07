package com.example.network.tcp;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

/**
 * TCP 通信示例 — BIO 模型
 *
 * 演示内容：
 * 1. TCP 服务端（ServerSocket）— 多线程处理客户端连接
 * 2. TCP 客户端（Socket）— 发送消息并接收响应
 * 3. BIO 模型的基本工作原理
 *
 * 知识点：
 * - TCP 是面向连接的可靠传输协议，通信前需要三次握手建立连接
 * - BIO（Blocking I/O）模型中，accept() 和 read() 都是阻塞操作
 * - 每个客户端连接需要一个独立线程处理，线程资源有限
 *
 * @see <a href="docs/2-framework/2.1-network/01-tcp-ip.md">TCP/IP 协议栈</a>
 */
public class TCPDemo {

    /** 服务端默认监听端口 */
    public static final int DEFAULT_PORT = 0; // 使用 0 让系统自动分配可用端口

    // ==================== TCP 服务端 ====================

    /**
     * BIO TCP 服务端
     * <p>
     * 工作流程：
     * 1. 创建 ServerSocket 绑定端口
     * 2. 调用 accept() 阻塞等待客户端连接（三次握手在此完成）
     * 3. 为每个客户端连接创建新线程处理
     * 4. 读取客户端消息，处理后返回响应
     */
    public static class TCPServer implements Closeable {

        private final ServerSocket serverSocket;
        private final ExecutorService threadPool;
        private volatile boolean running = true;

        public TCPServer(int port) throws IOException {
            // 创建 ServerSocket，backlog 参数控制全连接队列大小
            this.serverSocket = new ServerSocket(port);
            // 使用线程池管理客户端连接，避免无限创建线程
            this.threadPool = Executors.newFixedThreadPool(10);
        }

        /**
         * 获取服务端实际监听的端口号
         */
        public int getPort() {
            return serverSocket.getLocalPort();
        }

        /**
         * 启动服务端，开始接受客户端连接
         */
        public void start() {
            System.out.println("[服务端] 启动成功，监听端口: " + getPort());

            while (running) {
                try {
                    // accept() 阻塞等待客户端连接
                    // 当客户端发起 TCP 三次握手并完成后，accept() 返回
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("[服务端] 新客户端连接: "
                            + clientSocket.getRemoteSocketAddress());

                    // 将客户端处理任务提交到线程池
                    threadPool.submit(() -> handleClient(clientSocket));
                } catch (IOException e) {
                    if (running) {
                        System.err.println("[服务端] 接受连接异常: " + e.getMessage());
                    }
                }
            }
        }

        /**
         * 处理单个客户端连接
         */
        private void handleClient(Socket clientSocket) {
            try (clientSocket;
                 BufferedReader in = new BufferedReader(
                         new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(
                         clientSocket.getOutputStream(), true)) {

                String message;
                // read() 阻塞等待客户端发送数据
                while ((message = in.readLine()) != null) {
                    System.out.println("[服务端] 收到消息: " + message);

                    // 处理消息并返回响应
                    String response = processMessage(message);
                    out.println(response);
                    System.out.println("[服务端] 发送响应: " + response);

                    // 收到 "bye" 时关闭连接
                    if ("bye".equalsIgnoreCase(message)) {
                        break;
                    }
                }
            } catch (IOException e) {
                System.err.println("[服务端] 处理客户端异常: " + e.getMessage());
            }
            System.out.println("[服务端] 客户端断开连接");
        }

        /**
         * 处理消息的业务逻辑
         */
        static String processMessage(String message) {
            if (message == null || message.isBlank()) {
                return "ECHO: [empty]";
            }
            if ("bye".equalsIgnoreCase(message)) {
                return "BYE";
            }
            return "ECHO: " + message.toUpperCase();
        }

        @Override
        public void close() throws IOException {
            running = false;
            threadPool.shutdownNow();
            if (!serverSocket.isClosed()) {
                serverSocket.close();
            }
            System.out.println("[服务端] 已关闭");
        }
    }

    // ==================== TCP 客户端 ====================

    /**
     * TCP 客户端
     * <p>
     * 工作流程：
     * 1. 创建 Socket 连接服务端（触发三次握手）
     * 2. 发送消息
     * 3. 接收服务端响应
     * 4. 关闭连接（触发四次挥手）
     */
    public static class TCPClient implements Closeable {

        private final Socket socket;
        private final BufferedReader in;
        private final PrintWriter out;

        public TCPClient(String host, int port) throws IOException {
            // 创建 Socket 并连接服务端（三次握手在此完成）
            this.socket = new Socket(host, port);
            this.in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
            System.out.println("[客户端] 连接成功: " + host + ":" + port);
        }

        /**
         * 发送消息并接收响应
         *
         * @param message 要发送的消息
         * @return 服务端的响应
         */
        public String sendAndReceive(String message) throws IOException {
            out.println(message);
            return in.readLine();
        }

        @Override
        public void close() throws IOException {
            in.close();
            out.close();
            socket.close();
            System.out.println("[客户端] 连接已关闭");
        }
    }

    // ==================== 演示入口 ====================

    public static void main(String[] args) throws Exception {
        System.out.println("=== TCP 通信示例（BIO 模型）===\n");

        // 启动服务端（使用系统分配的端口）
        TCPServer server = new TCPServer(DEFAULT_PORT);
        int port = server.getPort();

        // 在新线程中运行服务端
        Thread serverThread = new Thread(server::start);
        serverThread.setDaemon(true);
        serverThread.start();

        // 等待服务端启动
        Thread.sleep(500);

        // 客户端连接并通信
        try (TCPClient client = new TCPClient("localhost", port)) {
            // 发送多条消息
            String[] messages = {"Hello, Server!", "TCP 三次握手", "bye"};
            for (String msg : messages) {
                String response = client.sendAndReceive(msg);
                System.out.println("[客户端] 发送: " + msg + " → 收到: " + response);
            }
        }

        // 关闭服务端
        server.close();
        System.out.println("\n=== 演示结束 ===");
    }
}
