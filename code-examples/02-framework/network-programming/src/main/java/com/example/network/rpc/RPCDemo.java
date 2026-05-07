package com.example.network.rpc;

import java.io.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

/**
 * 简易 RPC 框架实现
 *
 * 演示内容：
 * 1. RPC 调用的完整流程：动态代理 → 序列化 → 网络传输 → 反序列化 → 反射调用
 * 2. 客户端通过动态代理透明地调用远程服务
 * 3. 服务端通过反射执行实际的服务方法
 *
 * 核心组件：
 * - RPCRequest：RPC 请求对象（接口名、方法名、参数类型、参数值）
 * - RPCResponse：RPC 响应对象（返回值、异常信息）
 * - RPCServer：RPC 服务端（注册服务、监听请求、反射调用）
 * - RPCClient：RPC 客户端（动态代理、序列化、网络发送）
 *
 * @see <a href="docs/2-framework/2.1-network/07-rpc.md">RPC 框架原理</a>
 */
public class RPCDemo {

    // ==================== RPC 请求/响应对象 ====================

    /**
     * RPC 请求对象 — 封装远程调用的所有信息
     * <p>
     * 包含：接口名、方法名、参数类型、参数值
     * 通过 Java 序列化在网络上传输
     */
    public static class RPCRequest implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private final String interfaceName;  // 接口全限定名
        private final String methodName;     // 方法名
        private final Class<?>[] paramTypes; // 参数类型数组
        private final Object[] args;         // 参数值数组

        public RPCRequest(String interfaceName, String methodName,
                          Class<?>[] paramTypes, Object[] args) {
            this.interfaceName = interfaceName;
            this.methodName = methodName;
            this.paramTypes = paramTypes;
            this.args = args;
        }

        public String getInterfaceName() { return interfaceName; }
        public String getMethodName() { return methodName; }
        public Class<?>[] getParamTypes() { return paramTypes; }
        public Object[] getArgs() { return args; }

        @Override
        public String toString() {
            return interfaceName + "." + methodName + "(...)";
        }
    }

    /**
     * RPC 响应对象 — 封装远程调用的返回结果
     */
    public static class RPCResponse implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private final Object result;     // 返回值
        private final String exception;  // 异常信息（如果有）

        public RPCResponse(Object result, String exception) {
            this.result = result;
            this.exception = exception;
        }

        public static RPCResponse success(Object result) {
            return new RPCResponse(result, null);
        }

        public static RPCResponse error(String exception) {
            return new RPCResponse(null, exception);
        }

        public Object getResult() { return result; }
        public String getException() { return exception; }
        public boolean hasException() { return exception != null; }
    }

    // ==================== 服务接口定义 ====================

    /**
     * 示例服务接口 — HelloService
     */
    public interface HelloService {
        String sayHello(String name);
        String greet(String name, int times);
    }

    /**
     * 示例服务接口 — CalculatorService
     */
    public interface CalculatorService {
        int add(int a, int b);
        int multiply(int a, int b);
    }

    // ==================== 服务实现 ====================

    /**
     * HelloService 的具体实现
     */
    public static class HelloServiceImpl implements HelloService {
        @Override
        public String sayHello(String name) {
            return "Hello, " + name + "!";
        }

        @Override
        public String greet(String name, int times) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < times; i++) {
                if (i > 0) sb.append(" ");
                sb.append("Hi, ").append(name).append("!");
            }
            return sb.toString();
        }
    }

    /**
     * CalculatorService 的具体实现
     */
    public static class CalculatorServiceImpl implements CalculatorService {
        @Override
        public int add(int a, int b) {
            return a + b;
        }

        @Override
        public int multiply(int a, int b) {
            return a * b;
        }
    }

    // ==================== RPC 服务端 ====================

    /**
     * RPC 服务端
     * <p>
     * 工作流程：
     * 1. 注册服务接口与实现的映射关系
     * 2. 监听端口，接收客户端连接
     * 3. 反序列化 RPCRequest
     * 4. 根据接口名找到实现类，通过反射调用方法
     * 5. 将结果序列化为 RPCResponse 返回
     */
    public static class RPCServer implements Closeable {

        private final ServerSocket serverSocket;
        private final ConcurrentHashMap<String, Object> serviceMap = new ConcurrentHashMap<>();
        private final ExecutorService threadPool;
        private volatile boolean running = true;

        public RPCServer(int port) throws IOException {
            this.serverSocket = new ServerSocket(port);
            this.threadPool = Executors.newFixedThreadPool(5);
        }

        public int getPort() {
            return serverSocket.getLocalPort();
        }

        /**
         * 注册服务
         *
         * @param serviceInterface 服务接口 Class
         * @param serviceImpl      服务实现对象
         */
        public <T> void registerService(Class<T> serviceInterface, T serviceImpl) {
            serviceMap.put(serviceInterface.getName(), serviceImpl);
            System.out.println("[RPC服务端] 注册服务: " + serviceInterface.getName());
        }

        /**
         * 启动服务端，开始监听请求
         */
        public void start() {
            System.out.println("[RPC服务端] 启动成功，监听端口: " + getPort());

            while (running) {
                try {
                    Socket client = serverSocket.accept();
                    threadPool.submit(() -> handleRequest(client));
                } catch (IOException e) {
                    if (running) {
                        System.err.println("[RPC服务端] 接受连接异常: " + e.getMessage());
                    }
                }
            }
        }

        /**
         * 处理单个 RPC 请求
         */
        private void handleRequest(Socket client) {
            try (client;
                 ObjectInputStream in = new ObjectInputStream(client.getInputStream());
                 ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream())) {

                // 1. 反序列化请求
                RPCRequest request = (RPCRequest) in.readObject();
                System.out.println("[RPC服务端] 收到请求: " + request);

                // 2. 查找服务实现
                Object serviceImpl = serviceMap.get(request.getInterfaceName());
                if (serviceImpl == null) {
                    out.writeObject(RPCResponse.error(
                            "服务未找到: " + request.getInterfaceName()));
                    return;
                }

                // 3. 通过反射调用方法
                RPCResponse response = invokeMethod(serviceImpl, request);

                // 4. 序列化响应并返回
                out.writeObject(response);
                out.flush();

            } catch (Exception e) {
                System.err.println("[RPC服务端] 处理请求异常: " + e.getMessage());
            }
        }

        /**
         * 通过反射调用服务方法
         */
        private RPCResponse invokeMethod(Object serviceImpl, RPCRequest request) {
            try {
                // 获取方法对象
                Method method = serviceImpl.getClass().getMethod(
                        request.getMethodName(), request.getParamTypes());

                // 反射调用
                Object result = method.invoke(serviceImpl, request.getArgs());

                return RPCResponse.success(result);
            } catch (NoSuchMethodException e) {
                return RPCResponse.error("方法未找到: " + request.getMethodName());
            } catch (InvocationTargetException e) {
                return RPCResponse.error("方法执行异常: " + e.getTargetException().getMessage());
            } catch (Exception e) {
                return RPCResponse.error("调用异常: " + e.getMessage());
            }
        }

        @Override
        public void close() throws IOException {
            running = false;
            threadPool.shutdownNow();
            if (!serverSocket.isClosed()) {
                serverSocket.close();
            }
            System.out.println("[RPC服务端] 已关闭");
        }
    }

    // ==================== RPC 客户端 ====================

    /**
     * RPC 客户端
     * <p>
     * 核心：通过 JDK 动态代理生成服务接口的代理对象。
     * 当调用代理对象的方法时，实际执行的是：
     * 1. 将方法调用信息封装为 RPCRequest
     * 2. 序列化后通过 Socket 发送到服务端
     * 3. 接收服务端返回的 RPCResponse
     * 4. 反序列化得到返回值
     */
    public static class RPCClient {

        private final String host;
        private final int port;

        public RPCClient(String host, int port) {
            this.host = host;
            this.port = port;
        }

        /**
         * 获取服务代理对象
         * <p>
         * 使用 JDK 动态代理，让远程调用看起来像本地方法调用
         *
         * @param serviceInterface 服务接口 Class
         * @return 代理对象
         */
        @SuppressWarnings("unchecked")
        public <T> T getProxy(Class<T> serviceInterface) {
            return (T) Proxy.newProxyInstance(
                    serviceInterface.getClassLoader(),
                    new Class<?>[]{serviceInterface},
                    new RPCInvocationHandler(host, port, serviceInterface.getName())
            );
        }
    }

    /**
     * RPC 调用处理器 — JDK 动态代理的核心
     * <p>
     * 拦截代理对象的方法调用，转换为网络请求
     */
    static class RPCInvocationHandler implements InvocationHandler {

        private final String host;
        private final int port;
        private final String interfaceName;

        RPCInvocationHandler(String host, int port, String interfaceName) {
            this.host = host;
            this.port = port;
            this.interfaceName = interfaceName;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // 跳过 Object 类的方法（toString、hashCode 等）
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(this, args);
            }

            // 1. 构建 RPC 请求
            RPCRequest request = new RPCRequest(
                    interfaceName,
                    method.getName(),
                    method.getParameterTypes(),
                    args
            );

            // 2. 通过 Socket 发送请求并接收响应
            return sendRequest(request);
        }

        /**
         * 发送 RPC 请求并接收响应
         */
        private Object sendRequest(RPCRequest request) throws Exception {
            try (Socket socket = new Socket(host, port);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                // 序列化请求并发送
                out.writeObject(request);
                out.flush();

                // 接收并反序列化响应
                RPCResponse response = (RPCResponse) in.readObject();

                if (response.hasException()) {
                    throw new RuntimeException("RPC 调用异常: " + response.getException());
                }

                return response.getResult();
            }
        }
    }

    // ==================== 演示入口 ====================

    public static void main(String[] args) throws Exception {
        System.out.println("=== 简易 RPC 框架示例 ===\n");

        // 1. 启动 RPC 服务端
        RPCServer server = new RPCServer(0);
        int port = server.getPort();

        // 注册服务
        server.registerService(HelloService.class, new HelloServiceImpl());
        server.registerService(CalculatorService.class, new CalculatorServiceImpl());

        // 在新线程中运行服务端
        Thread serverThread = new Thread(server::start);
        serverThread.setDaemon(true);
        serverThread.start();
        Thread.sleep(500);

        // 2. 创建 RPC 客户端
        RPCClient client = new RPCClient("localhost", port);

        // 3. 获取代理对象（像本地接口一样使用）
        HelloService helloProxy = client.getProxy(HelloService.class);
        CalculatorService calcProxy = client.getProxy(CalculatorService.class);

        // 4. 调用远程服务（对调用者来说，和调用本地方法完全一样）
        System.out.println("--- HelloService 调用 ---");
        String hello = helloProxy.sayHello("World");
        System.out.println("sayHello(\"World\") = " + hello);

        String greet = helloProxy.greet("Java", 3);
        System.out.println("greet(\"Java\", 3) = " + greet);

        System.out.println("\n--- CalculatorService 调用 ---");
        int sum = calcProxy.add(10, 20);
        System.out.println("add(10, 20) = " + sum);

        int product = calcProxy.multiply(6, 7);
        System.out.println("multiply(6, 7) = " + product);

        // 5. 关闭服务端
        server.close();
        System.out.println("\n=== 演示结束 ===");
    }
}
