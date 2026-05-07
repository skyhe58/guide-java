package com.example.network.tcp;

import org.junit.jupiter.api.*;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TCP 通信测试
 *
 * 验证 TCP 客户端/服务端通信的正确性
 */
class TCPTest {

    private TCPDemo.TCPServer server;
    private int port;

    @BeforeEach
    void setUp() throws Exception {
        // 使用系统分配的端口启动服务端
        server = new TCPDemo.TCPServer(0);
        port = server.getPort();

        Thread serverThread = new Thread(server::start);
        serverThread.setDaemon(true);
        serverThread.start();

        // 等待服务端启动
        Thread.sleep(300);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (server != null) {
            server.close();
        }
    }

    @Test
    @DisplayName("TCP 客户端发送消息，服务端回复 ECHO")
    void shouldEchoMessage() throws Exception {
        try (TCPDemo.TCPClient client = new TCPDemo.TCPClient("localhost", port)) {
            String response = client.sendAndReceive("Hello");
            assertEquals("ECHO: HELLO", response);
        }
    }

    @Test
    @DisplayName("TCP 客户端发送 bye，服务端回复 BYE")
    void shouldHandleByeMessage() throws Exception {
        try (TCPDemo.TCPClient client = new TCPDemo.TCPClient("localhost", port)) {
            String response = client.sendAndReceive("bye");
            assertEquals("BYE", response);
        }
    }

    @Test
    @DisplayName("TCP 客户端发送多条消息")
    void shouldHandleMultipleMessages() throws Exception {
        try (TCPDemo.TCPClient client = new TCPDemo.TCPClient("localhost", port)) {
            assertEquals("ECHO: FIRST", client.sendAndReceive("first"));
            assertEquals("ECHO: SECOND", client.sendAndReceive("second"));
            assertEquals("ECHO: THIRD", client.sendAndReceive("third"));
        }
    }

    @Test
    @DisplayName("TCP 服务端消息处理逻辑")
    void shouldProcessMessageCorrectly() {
        assertEquals("ECHO: HELLO", TCPDemo.TCPServer.processMessage("Hello"));
        assertEquals("ECHO: TCP TEST", TCPDemo.TCPServer.processMessage("tcp test"));
        assertEquals("BYE", TCPDemo.TCPServer.processMessage("bye"));
        assertEquals("BYE", TCPDemo.TCPServer.processMessage("BYE"));
        assertEquals("ECHO: [empty]", TCPDemo.TCPServer.processMessage(""));
        assertEquals("ECHO: [empty]", TCPDemo.TCPServer.processMessage("  "));
    }

    @Test
    @DisplayName("多个客户端并发连接")
    void shouldHandleMultipleClients() throws Exception {
        // 同时创建多个客户端
        try (TCPDemo.TCPClient client1 = new TCPDemo.TCPClient("localhost", port);
             TCPDemo.TCPClient client2 = new TCPDemo.TCPClient("localhost", port)) {

            String response1 = client1.sendAndReceive("client1");
            String response2 = client2.sendAndReceive("client2");

            assertEquals("ECHO: CLIENT1", response1);
            assertEquals("ECHO: CLIENT2", response2);
        }
    }
}
