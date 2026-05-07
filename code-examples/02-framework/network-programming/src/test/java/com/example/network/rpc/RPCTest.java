package com.example.network.rpc;

import org.junit.jupiter.api.*;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RPC 调用测试
 *
 * 验证简易 RPC 框架的完整调用流程：
 * 动态代理 → 序列化 → 网络传输 → 反序列化 → 反射调用 → 返回结果
 */
class RPCTest {

    private RPCDemo.RPCServer server;
    private RPCDemo.RPCClient client;

    @BeforeEach
    void setUp() throws Exception {
        // 启动 RPC 服务端
        server = new RPCDemo.RPCServer(0);
        int port = server.getPort();

        // 注册服务
        server.registerService(RPCDemo.HelloService.class, new RPCDemo.HelloServiceImpl());
        server.registerService(RPCDemo.CalculatorService.class, new RPCDemo.CalculatorServiceImpl());

        // 在新线程中运行服务端
        Thread serverThread = new Thread(server::start);
        serverThread.setDaemon(true);
        serverThread.start();

        Thread.sleep(300);

        // 创建客户端
        client = new RPCDemo.RPCClient("localhost", port);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (server != null) {
            server.close();
        }
    }

    @Test
    @DisplayName("RPC 调用 HelloService.sayHello")
    void shouldCallSayHello() {
        RPCDemo.HelloService proxy = client.getProxy(RPCDemo.HelloService.class);
        String result = proxy.sayHello("World");
        assertEquals("Hello, World!", result);
    }

    @Test
    @DisplayName("RPC 调用 HelloService.greet 多参数方法")
    void shouldCallGreetWithMultipleParams() {
        RPCDemo.HelloService proxy = client.getProxy(RPCDemo.HelloService.class);
        String result = proxy.greet("Java", 2);
        assertEquals("Hi, Java! Hi, Java!", result);
    }

    @Test
    @DisplayName("RPC 调用 CalculatorService.add")
    void shouldCallAdd() {
        RPCDemo.CalculatorService proxy = client.getProxy(RPCDemo.CalculatorService.class);
        int result = proxy.add(10, 20);
        assertEquals(30, result);
    }

    @Test
    @DisplayName("RPC 调用 CalculatorService.multiply")
    void shouldCallMultiply() {
        RPCDemo.CalculatorService proxy = client.getProxy(RPCDemo.CalculatorService.class);
        int result = proxy.multiply(6, 7);
        assertEquals(42, result);
    }

    @Test
    @DisplayName("RPC 多次调用同一服务")
    void shouldHandleMultipleCalls() {
        RPCDemo.CalculatorService proxy = client.getProxy(RPCDemo.CalculatorService.class);

        assertEquals(3, proxy.add(1, 2));
        assertEquals(7, proxy.add(3, 4));
        assertEquals(15, proxy.add(7, 8));
    }

    @Test
    @DisplayName("RPC 调用不同服务")
    void shouldCallDifferentServices() {
        RPCDemo.HelloService helloProxy = client.getProxy(RPCDemo.HelloService.class);
        RPCDemo.CalculatorService calcProxy = client.getProxy(RPCDemo.CalculatorService.class);

        assertEquals("Hello, Test!", helloProxy.sayHello("Test"));
        assertEquals(100, calcProxy.multiply(10, 10));
    }

    @Test
    @DisplayName("RPC 调用边界值")
    void shouldHandleEdgeCases() {
        RPCDemo.HelloService helloProxy = client.getProxy(RPCDemo.HelloService.class);
        RPCDemo.CalculatorService calcProxy = client.getProxy(RPCDemo.CalculatorService.class);

        // 空字符串
        assertEquals("Hello, !", helloProxy.sayHello(""));

        // greet 次数为 1
        assertEquals("Hi, X!", helloProxy.greet("X", 1));

        // greet 次数为 0
        assertEquals("", helloProxy.greet("X", 0));

        // 计算器：0 值
        assertEquals(0, calcProxy.add(0, 0));
        assertEquals(0, calcProxy.multiply(100, 0));

        // 负数
        assertEquals(-5, calcProxy.add(-10, 5));
        assertEquals(-42, calcProxy.multiply(-6, 7));
    }
}
