---
title: "AI Agent 开发"
module: "ai"
difficulty: "advanced"
interviewFrequency: "medium"
tags:
  - "AI Agent"
  - "Function Calling"
  - "Tool Use"
  - "ReAct"
codeExample: "07-ai/ai-examples/src/main/java/com/example/ai/agent/AgentDemo.java"
relatedEntries:
  - "/7-ai/7.1-ai/02-llm-integration"
  - "/7-ai/7.1-ai/05-prompt"
prerequisites:
  - "/7-ai/7.1-ai/05-prompt"
estimatedTime: "40min"
---

# AI Agent 开发

## 概念说明

AI Agent 是能够自主决策和执行任务的智能体。与简单的 LLM 对话不同，Agent 可以调用外部工具（API、数据库、搜索引擎等）来完成复杂任务。Function Calling 是实现 Agent 的核心技术，让 LLM 能够"调用函数"。

## 核心原理

### Agent 架构

```mermaid
graph TB
    subgraph "AI Agent"
        LLM["LLM（大脑）<br/>理解意图、规划步骤"]
        TOOLS["工具集<br/>API/数据库/搜索"]
        MEM["记忆<br/>对话历史/上下文"]
    end

    USER["用户"] -->|提问| LLM
    LLM -->|决定调用| TOOLS
    TOOLS -->|返回结果| LLM
    LLM -->|记录| MEM
    MEM -->|提供上下文| LLM
    LLM -->|回答| USER

    style LLM fill:#e1f5fe
    style TOOLS fill:#e8f5e9
    style MEM fill:#fff3e0
```

### Function Calling 流程

```mermaid
sequenceDiagram
    participant User as 用户
    participant Agent as Agent
    participant LLM as LLM
    participant Tool as 工具

    User->>Agent: "北京今天天气怎么样？"
    Agent->>LLM: 用户消息 + 可用工具列表
    LLM-->>Agent: 决定调用 getWeather(city="北京")
    Agent->>Tool: 执行 getWeather("北京")
    Tool-->>Agent: {"temp": 25, "weather": "晴"}
    Agent->>LLM: 工具返回结果
    LLM-->>Agent: "北京今天天气晴朗，气温25°C"
    Agent-->>User: 最终回答
```

### ReAct 模式

ReAct（Reasoning + Acting）是一种让 Agent 交替进行推理和行动的模式：

```mermaid
graph LR
    A["思考 Thought"] --> B["行动 Action"]
    B --> C["观察 Observation"]
    C --> A
    C -->|任务完成| D["最终回答"]

    style A fill:#e1f5fe
    style B fill:#e8f5e9
    style C fill:#fff3e0
```

```
Thought: 用户问北京天气，我需要调用天气 API
Action: getWeather(city="北京")
Observation: {"temp": 25, "weather": "晴"}
Thought: 已获取天气信息，可以回答用户
Answer: 北京今天天气晴朗，气温25°C，适合出行。
```

### 多 Agent 协作

```mermaid
graph TB
    O["编排 Agent<br/>（Orchestrator）"] --> A1["搜索 Agent"]
    O --> A2["分析 Agent"]
    O --> A3["写作 Agent"]

    A1 -->|搜索结果| O
    A2 -->|分析报告| O
    A3 -->|最终文档| O

    style O fill:#fce4ec
```

## 代码示例

### Function Calling 模拟

```java
/**
 * 模拟 AI Agent 的 Function Calling 机制
 * 演示工具注册、意图识别、工具调用
 */
public class AgentDemo {

    // 工具注册表
    private final Map<String, Function<Map<String, String>, String>> tools = new HashMap<>();

    // 注册工具
    public void registerTool(String name, Function<Map<String, String>, String> handler) {
        tools.put(name, handler);
    }

    // 模拟 Agent 处理流程
    public String process(String userMessage) {
        // 1. 意图识别（模拟 LLM 判断是否需要调用工具）
        String toolName = identifyTool(userMessage);

        if (toolName != null && tools.containsKey(toolName)) {
            // 2. 提取参数
            Map<String, String> params = extractParams(userMessage);
            // 3. 调用工具
            String toolResult = tools.get(toolName).apply(params);
            // 4. 基于工具结果生成回答
            return generateAnswer(userMessage, toolResult);
        }

        // 不需要工具，直接回答
        return directAnswer(userMessage);
    }
}
```

> 💻 完整代码示例：[code-examples/07-ai/ai-examples/src/main/java/com/example/ai/agent/AgentDemo.java](../../../code-examples/07-ai/ai-examples/src/main/java/com/example/ai/agent/AgentDemo.java)

## 常见面试题

### Q1: 什么是 AI Agent？和普通 LLM 对话有什么区别？

**难度**：⭐⭐⭐ | **频率**：🔥🔥

**标准答案**：

AI Agent 是能够自主决策和执行任务的智能体，核心区别在于 Agent 可以调用外部工具。普通 LLM 对话只能基于训练数据生成文本，无法获取实时信息或执行操作。Agent 通过 Function Calling 机制，让 LLM 判断何时需要调用什么工具，获取工具返回结果后再生成最终回答。常见的 Agent 模式有 ReAct（推理+行动交替）和多 Agent 协作。

**深入追问**：

- Function Calling 的原理是什么？
- ReAct 模式和 Plan-and-Execute 模式的区别？

## 参考资料

- [ReAct 论文](https://arxiv.org/abs/2210.03629)
- [OpenAI Function Calling](https://platform.openai.com/docs/guides/function-calling)
