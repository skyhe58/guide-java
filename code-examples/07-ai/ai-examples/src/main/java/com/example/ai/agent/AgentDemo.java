package com.example.ai.agent;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;

/**
 * AI Agent Function Calling 模拟演示
 *
 * 演示核心概念：
 * 1. 工具注册与管理
 * 2. 意图识别（模拟 LLM 判断是否需要调用工具）
 * 3. 参数提取
 * 4. 工具调用与结果整合
 * 5. ReAct 模式（推理 + 行动）
 *
 * 注意：本示例不依赖实际 LLM API，用模拟方式演示 Agent 核心逻辑
 */
public class AgentDemo {

    // ==================== 数据模型 ====================

    /** 工具定义 */
    public record ToolDefinition(
            String name,
            String description,
            List<String> parameters,
            Function<Map<String, String>, String> handler
    ) {}

    /** Agent 执行步骤（ReAct 模式） */
    public record AgentStep(String thought, String action, String observation) {}

    // ==================== Agent 核心 ====================

    private final Map<String, ToolDefinition> tools = new LinkedHashMap<>();
    private final List<AgentStep> executionTrace = new ArrayList<>();

    /** 注册工具 */
    public void registerTool(String name, String description,
                              List<String> parameters,
                              Function<Map<String, String>, String> handler) {
        tools.put(name, new ToolDefinition(name, description, parameters, handler));
    }

    /**
     * Agent 处理用户请求（ReAct 模式）
     *
     * ReAct 循环：
     * 1. Thought（思考）：分析用户意图，决定下一步行动
     * 2. Action（行动）：调用工具或直接回答
     * 3. Observation（观察）：获取工具返回结果
     * 重复直到可以给出最终回答
     */
    public String process(String userMessage) {
        executionTrace.clear();

        // 步骤 1：思考 - 识别意图和需要的工具
        String toolName = identifyTool(userMessage);

        if (toolName != null && tools.containsKey(toolName)) {
            // 步骤 2：行动 - 提取参数并调用工具
            Map<String, String> params = extractParams(userMessage, toolName);
            String toolResult = tools.get(toolName).handler().apply(params);

            // 记录执行轨迹
            executionTrace.add(new AgentStep(
                    "用户需要 " + tools.get(toolName).description() + "，需要调用 " + toolName + " 工具",
                    toolName + "(" + params + ")",
                    toolResult
            ));

            // 步骤 3：观察 - 基于工具结果生成最终回答
            return generateFinalAnswer(userMessage, toolResult);
        }

        // 不需要工具，直接回答
        executionTrace.add(new AgentStep(
                "这个问题可以直接回答，不需要调用工具",
                "direct_answer",
                "无需工具调用"
        ));
        return directAnswer(userMessage);
    }

    /** 获取执行轨迹（用于调试和展示） */
    public List<AgentStep> getExecutionTrace() {
        return Collections.unmodifiableList(executionTrace);
    }

    // ==================== 模拟方法 ====================

    /** 模拟意图识别（实际由 LLM 完成） */
    private String identifyTool(String message) {
        String lower = message.toLowerCase();
        if (lower.contains("天气") || lower.contains("weather")) return "getWeather";
        if (lower.contains("时间") || lower.contains("几点") || lower.contains("time")) return "getCurrentTime";
        if (lower.contains("计算") || lower.contains("加") || lower.contains("减")) return "calculate";
        if (lower.contains("搜索") || lower.contains("查找") || lower.contains("search")) return "search";
        return null;
    }

    /** 模拟参数提取（实际由 LLM 完成） */
    private Map<String, String> extractParams(String message, String toolName) {
        Map<String, String> params = new HashMap<>();
        switch (toolName) {
            case "getWeather" -> {
                if (message.contains("北京")) params.put("city", "北京");
                else if (message.contains("上海")) params.put("city", "上海");
                else params.put("city", "未知城市");
            }
            case "calculate" -> {
                // 简单提取数字
                params.put("expression", message.replaceAll("[^0-9+\\-*/]", ""));
            }
            case "search" -> {
                params.put("query", message.replace("搜索", "").replace("查找", "").trim());
            }
        }
        return params;
    }

    /** 模拟基于工具结果生成最终回答 */
    private String generateFinalAnswer(String question, String toolResult) {
        return "根据查询结果：" + toolResult + "\n综合以上信息，回答您的问题「" + question + "」。";
    }

    /** 模拟直接回答 */
    private String directAnswer(String message) {
        return "[直接回答] 这是一个不需要工具调用的问题。在实际应用中，LLM 会直接生成回答。";
    }

    // ==================== 演示入口 ====================

    public static void main(String[] args) {
        System.out.println("=== AI Agent Function Calling 演示 ===\n");

        AgentDemo agent = new AgentDemo();

        // 注册工具
        agent.registerTool("getWeather", "查询天气信息",
                List.of("city"),
                params -> {
                    String city = params.getOrDefault("city", "未知");
                    // 模拟天气 API 返回
                    return switch (city) {
                        case "北京" -> "北京：晴，25°C，湿度 40%，空气质量良";
                        case "上海" -> "上海：多云，28°C，湿度 65%，空气质量优";
                        default -> city + "：暂无天气数据";
                    };
                });

        agent.registerTool("getCurrentTime", "获取当前时间",
                List.of(),
                params -> LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        agent.registerTool("calculate", "数学计算",
                List.of("expression"),
                params -> {
                    String expr = params.getOrDefault("expression", "0");
                    return "计算结果: " + expr + " (模拟计算)";
                });

        agent.registerTool("search", "搜索信息",
                List.of("query"),
                params -> "搜索「" + params.getOrDefault("query", "") + "」的结果：找到 3 条相关信息。");

        // 显示已注册的工具
        System.out.println("已注册工具:");
        agent.tools.forEach((name, tool) ->
                System.out.println("  - " + name + ": " + tool.description()));

        // 测试不同类型的请求
        System.out.println("\n--- 测试 1：需要调用工具 ---");
        String answer1 = agent.process("北京今天天气怎么样？");
        System.out.println("回答: " + answer1);
        printTrace(agent);

        System.out.println("\n--- 测试 2：需要调用工具 ---");
        String answer2 = agent.process("现在几点了？");
        System.out.println("回答: " + answer2);
        printTrace(agent);

        System.out.println("\n--- 测试 3：不需要工具 ---");
        String answer3 = agent.process("什么是 Java？");
        System.out.println("回答: " + answer3);
        printTrace(agent);
    }

    private static void printTrace(AgentDemo agent) {
        System.out.println("执行轨迹:");
        for (AgentStep step : agent.getExecutionTrace()) {
            System.out.println("  Thought: " + step.thought());
            System.out.println("  Action: " + step.action());
            System.out.println("  Observation: " + step.observation());
        }
    }
}
