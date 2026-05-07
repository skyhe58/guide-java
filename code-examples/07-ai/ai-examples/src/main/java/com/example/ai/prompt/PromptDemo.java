package com.example.ai.prompt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Prompt Engineering 演示
 *
 * 演示核心概念：
 * 1. Prompt 模板管理（变量替换）
 * 2. Few-shot 示例构建
 * 3. System Prompt 角色设定
 * 4. Chain-of-Thought 思维链
 *
 * 注意：本示例不依赖实际 LLM API，用模拟方式演示 Prompt 构建逻辑
 */
public class PromptDemo {

    // ==================== 1. Prompt 模板管理 ====================

    /**
     * 简单模板引擎：支持 {variable} 占位符替换
     * 实际使用 Spring AI 时：new PromptTemplate(template).render(variables)
     */
    public static String render(String template, Map<String, String> variables) {
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    // ==================== 2. 预定义模板 ====================

    /** 代码审查模板 */
    public static final String CODE_REVIEW_TEMPLATE = """
            你是一个资深 Java 代码审查专家。
            请审查以下代码，指出潜在问题和改进建议。

            代码语言：{language}
            代码内容：
            ```
            {code}
            ```

            请按以下格式输出：
            1. 问题描述
            2. 严重程度（高/中/低）
            3. 改进建议
            """;

    /** SQL 生成模板 */
    public static final String SQL_GENERATION_TEMPLATE = """
            你是一个 SQL 专家。根据以下表结构和需求，生成 SQL 查询语句。

            表结构：
            {schema}

            需求：{requirement}

            请生成优化的 SQL 查询，并解释查询逻辑。
            """;

    /** 技术文档生成模板 */
    public static final String DOC_GENERATION_TEMPLATE = """
            请为以下 Java 类生成技术文档，包含：
            1. 类的用途说明
            2. 核心方法说明
            3. 使用示例

            类名：{className}
            代码：
            ```java
            {code}
            ```
            """;

    // ==================== 3. Few-shot 示例构建 ====================

    /** Few-shot 示例 */
    public record FewShotExample(String input, String output) {}

    /**
     * 构建 Few-shot Prompt
     * 通过提供输入输出示例，引导 LLM 按照期望格式回答
     */
    public static String buildFewShotPrompt(String systemPrompt,
                                             List<FewShotExample> examples,
                                             String userInput) {
        StringBuilder sb = new StringBuilder();
        sb.append(systemPrompt).append("\n\n");

        for (int i = 0; i < examples.size(); i++) {
            sb.append("示例 ").append(i + 1).append("：\n");
            sb.append("输入：").append(examples.get(i).input()).append("\n");
            sb.append("输出：").append(examples.get(i).output()).append("\n\n");
        }

        sb.append("现在请处理以下输入：\n");
        sb.append("输入：").append(userInput).append("\n");
        sb.append("输出：");

        return sb.toString();
    }

    // ==================== 4. Chain-of-Thought ====================

    /**
     * 构建 Chain-of-Thought Prompt
     * 要求 LLM 逐步推理，提升复杂问题的准确性
     */
    public static String buildCoTPrompt(String question, List<String> steps) {
        StringBuilder sb = new StringBuilder();
        sb.append(question).append("\n\n");
        sb.append("请按以下步骤逐步分析：\n");

        for (int i = 0; i < steps.size(); i++) {
            sb.append(i + 1).append(". ").append(steps.get(i)).append("\n");
        }

        sb.append("\n请逐步给出你的分析过程和最终答案。");
        return sb.toString();
    }

    // ==================== 演示入口 ====================

    public static void main(String[] args) {
        System.out.println("=== Prompt Engineering 演示 ===\n");

        // 1. 模板渲染
        System.out.println("--- 1. 模板渲染 ---");
        Map<String, String> vars = new HashMap<>();
        vars.put("language", "Java");
        vars.put("code", "String s = new String(\"hello\");");
        String prompt = render(CODE_REVIEW_TEMPLATE, vars);
        System.out.println(prompt);

        // 2. Few-shot 示例
        System.out.println("--- 2. Few-shot Prompt ---");
        List<FewShotExample> examples = List.of(
                new FewShotExample(
                        "String s = new String(\"hello\");",
                        "⚠️ 不推荐。直接使用字符串字面量 \"hello\" 即可。"),
                new FewShotExample(
                        "if (list.size() > 0) { }",
                        "💡 建议使用 !list.isEmpty() 替代，语义更清晰。")
        );
        String fewShotPrompt = buildFewShotPrompt(
                "你是一个 Java 代码审查助手。请按示例格式审查代码。",
                examples,
                "Map map = new HashMap();"
        );
        System.out.println(fewShotPrompt);

        // 3. Chain-of-Thought
        System.out.println("--- 3. Chain-of-Thought Prompt ---");
        String cotPrompt = buildCoTPrompt(
                "分析以下代码的时间复杂度：\nfor(int i=0; i<n; i++) { for(int j=i; j<n; j++) { } }",
                List.of(
                        "外层循环执行次数",
                        "内层循环在每次外层迭代中的执行次数",
                        "总执行次数的数学表达式",
                        "最终时间复杂度"
                )
        );
        System.out.println(cotPrompt);
    }
}
