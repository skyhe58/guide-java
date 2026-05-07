package com.example.ai.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * LLM API 调用模拟演示
 *
 * 演示核心概念：
 * 1. 消息角色（system/user/assistant）
 * 2. 同步调用与流式调用
 * 3. Token 使用统计
 *
 * 注意：本示例不依赖实际 LLM API，用模拟方式演示核心逻辑
 */
public class ChatDemo {

    // ==================== 数据模型 ====================

    /** 消息角色 */
    public enum Role {
        SYSTEM,     // 系统提示（设定 AI 角色和行为）
        USER,       // 用户输入
        ASSISTANT   // AI 回复
    }

    /** 聊天消息 */
    public record Message(Role role, String content) {
        public static Message system(String content) {
            return new Message(Role.SYSTEM, content);
        }
        public static Message user(String content) {
            return new Message(Role.USER, content);
        }
        public static Message assistant(String content) {
            return new Message(Role.ASSISTANT, content);
        }
    }

    /** Token 使用统计 */
    public record Usage(int promptTokens, int completionTokens) {
        public int totalTokens() {
            return promptTokens + completionTokens;
        }
    }

    /** 聊天响应 */
    public record ChatResponse(String content, Usage usage) {}

    // ==================== 核心方法 ====================

    /**
     * 模拟同步聊天调用
     * 实际使用 Spring AI 时：
     *   chatClient.prompt().user(message).call().content()
     */
    public static ChatResponse chat(List<Message> messages) {
        // 提取用户最后一条消息
        String userMessage = messages.stream()
                .filter(m -> m.role() == Role.USER)
                .reduce((first, second) -> second)
                .map(Message::content)
                .orElse("");

        // 模拟 LLM 生成响应
        String response = generateResponse(userMessage);

        // 模拟 Token 统计
        int promptTokens = estimateTokens(messages);
        int completionTokens = estimateTokens(response);

        return new ChatResponse(response, new Usage(promptTokens, completionTokens));
    }

    /**
     * 模拟流式聊天调用
     * 实际使用 Spring AI 时：
     *   chatClient.prompt().user(message).stream().content()
     *
     * @param userMessage 用户消息
     * @param onToken     每个 token 的回调（模拟 SSE 流式推送）
     */
    public static void streamChat(String userMessage, Consumer<String> onToken) {
        String fullResponse = generateResponse(userMessage);

        // 模拟逐字输出（实际是逐 token）
        for (char c : fullResponse.toCharArray()) {
            onToken.accept(String.valueOf(c));
            try {
                Thread.sleep(50); // 模拟网络延迟
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * 模拟多轮对话（维护对话历史）
     */
    public static List<Message> multiTurnChat(List<Message> history, String userMessage) {
        List<Message> messages = new ArrayList<>(history);
        messages.add(Message.user(userMessage));

        ChatResponse response = chat(messages);
        messages.add(Message.assistant(response.content()));

        return messages;
    }

    // ==================== 辅助方法 ====================

    /** 模拟 LLM 响应生成 */
    private static String generateResponse(String userMessage) {
        if (userMessage.contains("Java")) {
            return "Java 是一种广泛使用的面向对象编程语言，具有跨平台、强类型、自动内存管理等特点。";
        } else if (userMessage.contains("Spring")) {
            return "Spring 是 Java 生态中最流行的应用框架，核心特性包括 IoC 容器和 AOP。";
        } else if (userMessage.contains("Docker")) {
            return "Docker 是一个容器化平台，通过将应用打包为镜像来实现一致的部署环境。";
        }
        return "这是一个模拟的 LLM 响应。在实际应用中，这里会调用 OpenAI 或其他大模型 API。";
    }

    /** 估算 Token 数量（简化：中文约 1 字 = 1-2 token） */
    private static int estimateTokens(String text) {
        return (int) (text.length() * 1.5);
    }

    private static int estimateTokens(List<Message> messages) {
        return messages.stream()
                .mapToInt(m -> estimateTokens(m.content()))
                .sum();
    }

    // ==================== 演示入口 ====================

    public static void main(String[] args) {
        System.out.println("=== LLM API 调用模拟演示 ===\n");

        // 1. 同步调用
        System.out.println("--- 同步调用 ---");
        List<Message> messages = List.of(
                Message.system("你是一个 Java 技术专家"),
                Message.user("简单介绍一下 Java 语言")
        );
        ChatResponse response = chat(messages);
        System.out.println("回复: " + response.content());
        System.out.println("Token 使用: prompt=" + response.usage().promptTokens()
                + ", completion=" + response.usage().completionTokens()
                + ", total=" + response.usage().totalTokens());

        // 2. 流式调用
        System.out.println("\n--- 流式调用 ---");
        System.out.print("回复: ");
        streamChat("介绍一下 Spring 框架", token -> System.out.print(token));
        System.out.println();

        // 3. 多轮对话
        System.out.println("\n--- 多轮对话 ---");
        List<Message> history = new ArrayList<>();
        history.add(Message.system("你是一个 Java 技术专家"));

        history = multiTurnChat(history, "什么是 Docker？");
        System.out.println("第1轮回复: " + history.getLast().content());

        history = multiTurnChat(history, "它和虚拟机有什么区别？");
        System.out.println("第2轮回复: " + history.getLast().content());
    }
}
