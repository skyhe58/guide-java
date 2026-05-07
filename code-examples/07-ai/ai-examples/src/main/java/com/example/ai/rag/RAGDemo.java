package com.example.ai.rag;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG（检索增强生成）流程模拟演示
 *
 * 完整流程：文档加载 → 文本分块 → 向量化 → 存入向量库 → 检索 → 生成回答
 *
 * 注意：本示例不依赖实际 LLM API 和向量数据库，用模拟方式演示核心逻辑
 */
public class RAGDemo {

    // ==================== 数据模型 ====================

    /** 文档块 */
    public record DocumentChunk(String id, String content, double[] vector) {}

    /** 检索结果 */
    public record SearchResult(String content, double score) {}

    // ==================== 1. 文本分块 ====================

    /**
     * 固定大小分块（带重叠）
     *
     * @param text      原始文本
     * @param chunkSize 每块大小（字符数）
     * @param overlap   重叠区域大小
     * @return 文本块列表
     */
    public static List<String> splitText(String text, int chunkSize, int overlap) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        if (chunkSize <= overlap) {
            throw new IllegalArgumentException("chunkSize 必须大于 overlap");
        }

        List<String> chunks = new ArrayList<>();
        int step = chunkSize - overlap;

        for (int i = 0; i < text.length(); i += step) {
            int end = Math.min(i + chunkSize, text.length());
            chunks.add(text.substring(i, end));
            if (end == text.length()) break;
        }
        return chunks;
    }

    // ==================== 2. 向量化（模拟） ====================

    /**
     * 模拟文本向量化（Embedding）
     * 实际使用 Spring AI 时：embeddingModel.embed(text)
     *
     * 这里用简单的字符频率统计模拟向量，维度为 8
     */
    public static double[] embed(String text) {
        double[] vector = new double[8];
        if (text == null || text.isEmpty()) return vector;

        // 基于字符特征生成模拟向量
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            vector[i % 8] += c;
        }

        // 归一化
        double norm = 0;
        for (double v : vector) norm += v * v;
        norm = Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= norm;
            }
        }
        return vector;
    }

    // ==================== 3. 相似度计算 ====================

    /**
     * 余弦相似度计算
     * cos(A,B) = A·B / (||A|| × ||B||)
     */
    public static double cosineSimilarity(double[] a, double[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("向量维度不一致");
        }

        double dotProduct = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        double denominator = Math.sqrt(normA) * Math.sqrt(normB);
        return denominator == 0 ? 0 : dotProduct / denominator;
    }

    // ==================== 4. 向量存储与检索 ====================

    /** 模拟向量数据库 */
    private final List<DocumentChunk> vectorStore = new ArrayList<>();

    /** 添加文档到向量库 */
    public void addDocument(String id, String content) {
        double[] vector = embed(content);
        vectorStore.add(new DocumentChunk(id, content, vector));
    }

    /**
     * 相似度检索 Top-K
     *
     * @param query 查询文本
     * @param topK  返回最相似的 K 个结果
     * @return 检索结果列表（按相似度降序）
     */
    public List<SearchResult> search(String query, int topK) {
        double[] queryVector = embed(query);

        return vectorStore.stream()
                .map(chunk -> new SearchResult(
                        chunk.content(),
                        cosineSimilarity(queryVector, chunk.vector())))
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .limit(topK)
                .collect(Collectors.toList());
    }

    // ==================== 5. 生成回答（模拟） ====================

    /**
     * 模拟 RAG 生成回答
     * 实际使用时：将检索到的文档作为上下文拼入 Prompt，调用 LLM 生成
     */
    public String generateAnswer(String question, List<SearchResult> context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("基于以下参考资料回答问题：\n\n");

        for (int i = 0; i < context.size(); i++) {
            prompt.append("参考资料 ").append(i + 1).append("（相似度: ")
                    .append(String.format("%.4f", context.get(i).score()))
                    .append("）:\n")
                    .append(context.get(i).content()).append("\n\n");
        }

        prompt.append("问题: ").append(question).append("\n");
        prompt.append("回答: ");

        // 模拟 LLM 回答（实际会调用 ChatClient）
        return "[模拟回答] 根据检索到的 " + context.size() + " 条相关文档，"
                + "关于「" + question + "」的回答如下：" + context.getFirst().content();
    }

    // ==================== 完整 RAG 流程 ====================

    /**
     * 执行完整的 RAG 流程
     */
    public String rag(String question, int topK) {
        // 1. 检索相关文档
        List<SearchResult> results = search(question, topK);

        // 2. 生成回答
        return generateAnswer(question, results);
    }

    // ==================== 演示入口 ====================

    public static void main(String[] args) {
        System.out.println("=== RAG 检索增强生成演示 ===\n");

        RAGDemo ragDemo = new RAGDemo();

        // 1. 模拟知识库文档
        String[] documents = {
                "Java 是一种面向对象的编程语言，由 Sun Microsystems 于 1995 年发布。Java 具有跨平台特性，遵循 Write Once Run Anywhere 原则。",
                "Spring Boot 是基于 Spring 框架的快速开发工具，通过自动配置和约定优于配置的原则，简化了 Spring 应用的搭建和开发过程。",
                "Docker 是一个容器化平台，通过将应用及其依赖打包为镜像，实现一致的部署环境。Docker 使用 Namespace 和 Cgroup 实现容器隔离。",
                "Redis 是一个高性能的内存键值数据库，支持多种数据结构（String、Hash、List、Set、ZSet），常用于缓存、分布式锁和消息队列。",
                "Kubernetes 是容器编排平台，用于自动化部署、扩缩容和管理容器化应用。K8s 采用 Master-Node 架构。",
                "JVM 垃圾回收器包括 Serial、Parallel、CMS、G1 和 ZGC。G1 是 JDK 9+ 的默认收集器，适合大堆内存场景。"
        };

        // 2. 文本分块并索引
        System.out.println("--- 索引阶段 ---");
        for (int i = 0; i < documents.length; i++) {
            List<String> chunks = splitText(documents[i], 100, 20);
            for (int j = 0; j < chunks.size(); j++) {
                String chunkId = "doc" + i + "_chunk" + j;
                ragDemo.addDocument(chunkId, chunks.get(j));
                System.out.println("索引: " + chunkId + " (" + chunks.get(j).length() + " 字符)");
            }
        }

        // 3. 查询
        System.out.println("\n--- 查询阶段 ---");
        String question = "什么是容器化技术？";
        System.out.println("问题: " + question);

        List<SearchResult> results = ragDemo.search(question, 3);
        System.out.println("\n检索结果:");
        for (int i = 0; i < results.size(); i++) {
            System.out.printf("  Top-%d (相似度: %.4f): %s%n",
                    i + 1, results.get(i).score(),
                    results.get(i).content().substring(0, Math.min(50, results.get(i).content().length())) + "...");
        }

        // 4. 生成回答
        String answer = ragDemo.generateAnswer(question, results);
        System.out.println("\n最终回答: " + answer);
    }
}
