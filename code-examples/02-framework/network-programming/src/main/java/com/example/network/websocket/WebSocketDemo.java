package com.example.network.websocket;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.*;

/**
 * WebSocket 示例 — 简易服务端 + Java 11 客户端
 *
 * 演示内容：
 * 1. 简易 WebSocket 服务端（基于 ServerSocket 实现 HTTP Upgrade 握手）
 * 2. Java 11+ WebSocket 客户端（java.net.http.WebSocket）
 * 3. WebSocket 握手过程和帧通信
 *
 * 知识点：
 * - WebSocket 通过 HTTP Upgrade 机制建立连接
 * - 握手完成后切换为全双工的帧通信
 * - 客户端发送的帧必须掩码（MASK），服务端发送的帧不掩码
 *
 * @see <a href="docs/2-framework/2.1-network/03-websocket.md">WebSocket 协议</a>
 */
public class WebSocketDemo {

    /** WebSocket GUID，用于握手时计算 Sec-WebSocket-Accept */
    private static final String WS_GUID = "258EAFA5-E914-47DA-95CA-5AB5DC11AD35";

    // ==================== 简易 WebSocket 服务端 ====================

    /**
     * 简易 WebSocket 服务端
     * <p>
     * 实现了 WebSocket 握手和基本的文本帧收发。
     * 注意：这是一个教学用的简化实现，生产环境请使用 Netty、Tomcat 等成熟框架。
     */
    public static class SimpleWebSocketServer implements Closeable {

        private final ServerSocket serverSocket;
        private volatile boolean running = true;
        private final List<String> receivedMessages = new CopyOnWriteArrayList<>();

        public SimpleWebSocketServer(int port) throws IOException {
            this.serverSocket = new ServerSocket(port);
        }

        public int getPort() {
            return serverSocket.getLocalPort();
        }

        public List<String> getReceivedMessages() {
            return Collections.unmodifiableList(receivedMessages);
        }

        /**
         * 启动服务端，接受一个客户端连接并处理
         */
        public void acceptOneClient() {
            try {
                Socket client = serverSocket.accept();
                System.out.println("[WS服务端] 新连接: " + client.getRemoteSocketAddress());
                handleWebSocket(client);
            } catch (IOException e) {
                if (running) {
                    System.err.println("[WS服务端] 异常: " + e.getMessage());
                }
            }
        }

        /**
         * 处理 WebSocket 连接：握手 + 帧通信
         */
        private void handleWebSocket(Socket client) throws IOException {
            InputStream inputStream = client.getInputStream();
            OutputStream outputStream = client.getOutputStream();

            // 1. 读取 HTTP 升级请求
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream));
            String wsKey = readUpgradeRequest(reader);

            if (wsKey == null) {
                System.err.println("[WS服务端] 无效的 WebSocket 握手请求");
                client.close();
                return;
            }

            // 2. 发送 101 Switching Protocols 响应
            String acceptKey = computeAcceptKey(wsKey);
            String response = "HTTP/1.1 101 Switching Protocols\r\n"
                    + "Upgrade: websocket\r\n"
                    + "Connection: Upgrade\r\n"
                    + "Sec-WebSocket-Accept: " + acceptKey + "\r\n"
                    + "\r\n";
            outputStream.write(response.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
            System.out.println("[WS服务端] 握手完成，协议已升级为 WebSocket");

            // 3. 读取 WebSocket 帧
            try {
                while (running && !client.isClosed()) {
                    String message = readWebSocketFrame(inputStream);
                    if (message == null) {
                        break; // 连接关闭
                    }
                    System.out.println("[WS服务端] 收到消息: " + message);
                    receivedMessages.add(message);

                    // 回复消息（Echo）
                    String echoMessage = "Echo: " + message;
                    sendWebSocketFrame(outputStream, echoMessage);
                    System.out.println("[WS服务端] 发送响应: " + echoMessage);
                }
            } catch (IOException e) {
                System.out.println("[WS服务端] 连接关闭: " + e.getMessage());
            }

            client.close();
        }

        /**
         * 读取 HTTP 升级请求，提取 Sec-WebSocket-Key
         */
        private String readUpgradeRequest(BufferedReader reader) throws IOException {
            String wsKey = null;
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                if (line.startsWith("Sec-WebSocket-Key:")) {
                    wsKey = line.substring("Sec-WebSocket-Key:".length()).trim();
                }
            }
            return wsKey;
        }

        /**
         * 计算 Sec-WebSocket-Accept 值
         * 算法：SHA1(Sec-WebSocket-Key + GUID) → Base64
         */
        static String computeAcceptKey(String wsKey) {
            try {
                String combined = wsKey + WS_GUID;
                MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
                byte[] hash = sha1.digest(combined.getBytes(StandardCharsets.UTF_8));
                return Base64.getEncoder().encodeToString(hash);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("SHA-1 not available", e);
            }
        }

        /**
         * 读取一个 WebSocket 文本帧
         * <p>
         * 帧格式（简化版，仅处理小于 126 字节的文本帧）：
         * - 第 1 字节：FIN(1bit) + RSV(3bit) + opcode(4bit)
         * - 第 2 字节：MASK(1bit) + payload length(7bit)
         * - 如果 MASK=1，接下来 4 字节是掩码密钥
         * - 最后是负载数据
         */
        private String readWebSocketFrame(InputStream in) throws IOException {
            int firstByte = in.read();
            if (firstByte == -1) return null;

            int opcode = firstByte & 0x0F;
            // opcode 0x8 = 关闭帧
            if (opcode == 0x8) return null;

            int secondByte = in.read();
            if (secondByte == -1) return null;

            boolean masked = (secondByte & 0x80) != 0;
            int payloadLength = secondByte & 0x7F;

            // 处理扩展长度
            if (payloadLength == 126) {
                payloadLength = (in.read() << 8) | in.read();
            } else if (payloadLength == 127) {
                // 64-bit 长度，简化处理只读低 32 位
                in.read(); in.read(); in.read(); in.read();
                payloadLength = (in.read() << 24) | (in.read() << 16)
                        | (in.read() << 8) | in.read();
            }

            // 读取掩码密钥（客户端发送的帧必须掩码）
            byte[] maskKey = null;
            if (masked) {
                maskKey = in.readNBytes(4);
            }

            // 读取负载数据
            byte[] payload = in.readNBytes(payloadLength);

            // 解掩码
            if (masked && maskKey != null) {
                for (int i = 0; i < payload.length; i++) {
                    payload[i] ^= maskKey[i % 4];
                }
            }

            return new String(payload, StandardCharsets.UTF_8);
        }

        /**
         * 发送一个 WebSocket 文本帧（服务端发送不需要掩码）
         */
        private void sendWebSocketFrame(OutputStream out, String message) throws IOException {
            byte[] payload = message.getBytes(StandardCharsets.UTF_8);

            // FIN=1, opcode=0x1 (文本帧)
            out.write(0x81);

            // 负载长度（不掩码）
            if (payload.length < 126) {
                out.write(payload.length);
            } else if (payload.length < 65536) {
                out.write(126);
                out.write((payload.length >> 8) & 0xFF);
                out.write(payload.length & 0xFF);
            }

            out.write(payload);
            out.flush();
        }

        @Override
        public void close() throws IOException {
            running = false;
            if (!serverSocket.isClosed()) {
                serverSocket.close();
            }
            System.out.println("[WS服务端] 已关闭");
        }
    }

    // ==================== 演示入口 ====================

    public static void main(String[] args) throws Exception {
        System.out.println("=== WebSocket 通信示例 ===\n");

        // 启动简易 WebSocket 服务端
        SimpleWebSocketServer server = new SimpleWebSocketServer(0);
        int port = server.getPort();
        System.out.println("[演示] 服务端端口: " + port);

        // 在新线程中运行服务端
        Thread serverThread = new Thread(server::acceptOneClient);
        serverThread.setDaemon(true);
        serverThread.start();

        Thread.sleep(500);

        // 使用 Java 11 WebSocket 客户端连接
        CompletableFuture<String> receivedFuture = new CompletableFuture<>();

        HttpClient httpClient = HttpClient.newHttpClient();
        WebSocket ws = httpClient.newWebSocketBuilder()
                .buildAsync(URI.create("ws://localhost:" + port + "/chat"),
                        new WebSocket.Listener() {
                            @Override
                            public CompletionStage<?> onText(WebSocket webSocket,
                                                             CharSequence data, boolean last) {
                                System.out.println("[WS客户端] 收到消息: " + data);
                                receivedFuture.complete(data.toString());
                                // 请求下一条消息
                                webSocket.request(1);
                                return null;
                            }

                            @Override
                            public void onOpen(WebSocket webSocket) {
                                System.out.println("[WS客户端] 连接已建立");
                                webSocket.request(1);
                            }

                            @Override
                            public CompletionStage<?> onClose(WebSocket webSocket,
                                                              int statusCode, String reason) {
                                System.out.println("[WS客户端] 连接关闭: " + statusCode);
                                return null;
                            }
                        })
                .join();

        System.out.println("[WS客户端] 发送消息: Hello WebSocket!");
        ws.sendText("Hello WebSocket!", true);

        // 等待收到响应
        try {
            String received = receivedFuture.get(5, TimeUnit.SECONDS);
            System.out.println("[演示] 通信成功，收到: " + received);
        } catch (TimeoutException e) {
            System.out.println("[演示] 等待响应超时");
        }

        // 关闭连接
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "bye");
        server.close();

        System.out.println("\n=== 演示结束 ===");
    }
}
