package com.example.es.index_demo;

/**
 * Elasticsearch 倒排索引演示（混合模式）
 *
 * <p>Part A：用纯 Java 实现倒排索引的完整流程（直接运行）
 * <ul>
 *   <li>倒排索引构建：文档 → 分词 → 倒排表</li>
 *   <li>分词器模拟：Standard / 简单中文分词</li>
 *   <li>TF-IDF 相关性评分</li>
 *   <li>布尔查询在倒排索引上的执行过程</li>
 * </ul>
 *
 * <p>Part B：用 ES Java Client 演示索引管理 API
 *
 * <p>Part B 运行前启动依赖：
 * {@code docker compose -f docker/docker-compose.es.yml up -d elasticsearch}
 *
 * <h3>倒排索引结构：</h3>
 * <pre>
 *  正排索引（Forward Index）：文档 → 词项
 *    doc1 → ["Java", "并发", "编程"]
 *    doc2 → ["Java", "虚拟机", "GC"]
 *
 *  倒排索引（Inverted Index）：词项 → 文档列表
 *    "Java"   → [doc1, doc2]     （出现在 2 个文档中）
 *    "并发"   → [doc1]           （出现在 1 个文档中）
 *    "编程"   → [doc1]
 *    "虚拟机" → [doc2]
 *    "GC"     → [doc2]
 *
 *  倒排表（Posting List）的每个条目包含：
 *    - 文档 ID
 *    - 词频（TF：该词在文档中出现的次数）
 *    - 位置（Position：词在文档中的位置，用于短语查询）
 * </pre>
 */
public class IndexDemo {

    // ==================== 倒排索引实现 ====================

    /** 倒排表条目：记录一个词项在某个文档中的出现信息 */
    static class Posting {
        final String docId;
        final int termFrequency;                    // 词频（TF）
        final java.util.List<Integer> positions;    // 词在文档中的位置列表

        Posting(String docId, int termFrequency, java.util.List<Integer> positions) {
            this.docId = docId;
            this.termFrequency = termFrequency;
            this.positions = positions;
        }

        @Override
        public String toString() {
            return String.format("(doc=%s, tf=%d, pos=%s)", docId, termFrequency, positions);
        }
    }

    /** 倒排索引：词项 → 倒排表（Posting List） */
    static class InvertedIndex {
        // 倒排表：term → List<Posting>
        private final java.util.Map<String, java.util.List<Posting>> index = new java.util.LinkedHashMap<>();
        // 正排存储：docId → 原始文本
        private final java.util.Map<String, String> docStore = new java.util.LinkedHashMap<>();
        // 文档总数
        private int totalDocs = 0;

        /** 索引一个文档：分词 → 构建倒排表 */
        void indexDocument(String docId, String text) {
            docStore.put(docId, text);
            totalDocs++;

            // 分词
            java.util.List<String> tokens = tokenize(text);

            // 统计每个词的词频和位置
            java.util.Map<String, java.util.List<Integer>> termPositions = new java.util.LinkedHashMap<>();
            for (int i = 0; i < tokens.size(); i++) {
                String token = tokens.get(i);
                termPositions.computeIfAbsent(token, k -> new java.util.ArrayList<>()).add(i);
            }

            // 写入倒排表
            for (var entry : termPositions.entrySet()) {
                String term = entry.getKey();
                java.util.List<Integer> positions = entry.getValue();
                Posting posting = new Posting(docId, positions.size(), positions);
                index.computeIfAbsent(term, k -> new java.util.ArrayList<>()).add(posting);
            }
        }

        /** 查询：返回包含指定词项的文档列表 */
        java.util.List<Posting> search(String term) {
            return index.getOrDefault(term.toLowerCase(), java.util.Collections.emptyList());
        }

        /** 布尔 AND 查询：返回同时包含所有词项的文档 */
        java.util.Set<String> boolAndSearch(String... terms) {
            java.util.Set<String> result = null;
            for (String term : terms) {
                java.util.Set<String> docIds = new java.util.LinkedHashSet<>();
                for (Posting p : search(term)) {
                    docIds.add(p.docId);
                }
                if (result == null) {
                    result = docIds;
                } else {
                    result.retainAll(docIds); // 交集
                }
            }
            return result == null ? java.util.Collections.emptySet() : result;
        }

        /** 布尔 OR 查询：返回包含任一词项的文档 */
        java.util.Set<String> boolOrSearch(String... terms) {
            java.util.Set<String> result = new java.util.LinkedHashSet<>();
            for (String term : terms) {
                for (Posting p : search(term)) {
                    result.add(p.docId);
                }
            }
            return result;
        }

        /**
         * TF-IDF 评分搜索。
         * TF（词频）= 词在文档中出现的次数 / 文档总词数
         * IDF（逆文档频率）= log(文档总数 / 包含该词的文档数)
         * TF-IDF = TF × IDF
         */
        java.util.List<java.util.Map.Entry<String, Double>> searchWithTfIdf(String query) {
            java.util.List<String> queryTerms = tokenize(query);
            java.util.Map<String, Double> scores = new java.util.LinkedHashMap<>();

            for (String term : queryTerms) {
                java.util.List<Posting> postings = search(term);
                if (postings.isEmpty()) continue;

                // IDF = log(N / df)，N=文档总数，df=包含该词的文档数
                double idf = Math.log((double) totalDocs / postings.size());

                for (Posting posting : postings) {
                    // TF = 词频 / 文档总词数（简化：直接用词频）
                    double tf = posting.termFrequency;
                    double tfIdf = tf * idf;
                    scores.merge(posting.docId, tfIdf, Double::sum);
                }
            }

            // 按分数降序排列
            java.util.List<java.util.Map.Entry<String, Double>> sorted = new java.util.ArrayList<>(scores.entrySet());
            sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
            return sorted;
        }

        /** 简单分词器：按空格和标点分词，转小写 */
        static java.util.List<String> tokenize(String text) {
            java.util.List<String> tokens = new java.util.ArrayList<>();
            for (String word : text.split("[\\s,，。、；：！？()（）《》'']+")) {
                String trimmed = word.trim().toLowerCase();
                if (!trimmed.isEmpty() && trimmed.length() <= 20) {
                    tokens.add(trimmed);
                }
            }
            return tokens;
        }

        /** 打印倒排索引内容 */
        void printIndex() {
            for (var entry : index.entrySet()) {
                System.out.printf("    %-12s → %s%n", "\"" + entry.getKey() + "\"", entry.getValue());
            }
        }

        String getDoc(String docId) {
            return docStore.get(docId);
        }
    }

    // ==================== Part A 演示方法 ====================

    /** 演示1：倒排索引构建过程 */
    static void demoBuildIndex() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示1：倒排索引构建过程");
        System.out.println("═══════════════════════════════════════════════════");

        InvertedIndex idx = new InvertedIndex();
        idx.indexDocument("doc1", "Java 并发 编程 实战 多线程 锁");
        idx.indexDocument("doc2", "Java 虚拟机 GC 内存 类加载");
        idx.indexDocument("doc3", "Spring Boot Java Web 开发");
        idx.indexDocument("doc4", "MySQL 索引 B+树 Java 优化");

        System.out.println("\n  文档列表：");
        System.out.println("    doc1: Java 并发 编程 实战 多线程 锁");
        System.out.println("    doc2: Java 虚拟机 GC 内存 类加载");
        System.out.println("    doc3: Spring Boot Java Web 开发");
        System.out.println("    doc4: MySQL 索引 B+树 Java 优化");

        System.out.println("\n  倒排索引：");
        idx.printIndex();
        System.out.println();
    }

    /** 演示2：倒排索引查询 */
    static void demoSearch() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示2：倒排索引查询过程");
        System.out.println("═══════════════════════════════════════════════════");

        InvertedIndex idx = new InvertedIndex();
        idx.indexDocument("doc1", "Java 并发 编程 实战 多线程 锁");
        idx.indexDocument("doc2", "Java 虚拟机 GC 内存 类加载");
        idx.indexDocument("doc3", "Spring Boot Java Web 开发");
        idx.indexDocument("doc4", "MySQL 索引 B+树 Java 优化");

        // 单词查询
        System.out.println("\n  【单词查询】搜索 'java'：");
        for (Posting p : idx.search("java")) {
            System.out.printf("    %s (tf=%d) → %s%n", p.docId, p.termFrequency, idx.getDoc(p.docId));
        }

        // AND 查询
        System.out.println("\n  【AND 查询】搜索 'java AND 优化'：");
        for (String docId : idx.boolAndSearch("java", "优化")) {
            System.out.printf("    %s → %s%n", docId, idx.getDoc(docId));
        }

        // OR 查询
        System.out.println("\n  【OR 查询】搜索 'gc OR 并发'：");
        for (String docId : idx.boolOrSearch("gc", "并发")) {
            System.out.printf("    %s → %s%n", docId, idx.getDoc(docId));
        }
        System.out.println();
    }

    /** 演示3：TF-IDF 相关性评分 */
    static void demoTfIdf() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示3：TF-IDF 相关性评分");
        System.out.println("═══════════════════════════════════════════════════");

        InvertedIndex idx = new InvertedIndex();
        idx.indexDocument("doc1", "Java Java 并发 编程 Java 多线程");
        idx.indexDocument("doc2", "Java 虚拟机 GC 内存");
        idx.indexDocument("doc3", "Spring Boot Web 开发");
        idx.indexDocument("doc4", "MySQL 索引 优化 Java");

        System.out.println("\n  搜索 'Java' 的 TF-IDF 评分：");
        System.out.println("  公式：TF-IDF = TF(词频) × IDF(log(N/df))");
        System.out.printf("  N=%d, df(java)=%d, IDF=log(%d/%d)=%.4f%n",
                4, 3, 4, 3, Math.log(4.0 / 3));

        var results = idx.searchWithTfIdf("Java");
        for (var entry : results) {
            System.out.printf("    %s  score=%.4f  → %s%n",
                    entry.getKey(), entry.getValue(), idx.getDoc(entry.getKey()));
        }

        System.out.println("\n  doc1 分数最高，因为 'Java' 出现了 3 次（TF=3）");
        System.out.println("  doc3 不在结果中，因为不包含 'Java'");
        System.out.println();
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  Elasticsearch 倒排索引演示（混合模式）                ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        // ===== Part A：纯内存模拟 =====
        System.out.println("══════════ Part A：手写倒排索引 ══════════");
        System.out.println();
        demoBuildIndex();
        demoSearch();
        demoTfIdf();

        // ===== Part B：连接真实 ES =====
        if (args.length > 0 && "real".equals(args[0])) {
            System.out.println("══════════ Part B：ES 索引管理 API ══════════");
            System.out.println();
            RealEsIndex.run();
        } else {
            System.out.println("提示：运行 Part B（ES 索引管理）请传入参数 'real'");
        }
    }

    // ==================== Part B：真实 ES 索引管理 ====================

    static class RealEsIndex {

        static void run() throws Exception {
            org.apache.http.HttpHost host = new org.apache.http.HttpHost("localhost", 9200, "http");
            org.elasticsearch.client.RestClient restClient =
                    org.elasticsearch.client.RestClient.builder(host).build();
            co.elastic.clients.transport.ElasticsearchTransport transport =
                    new co.elastic.clients.transport.rest_client.RestClientTransport(
                            restClient, new co.elastic.clients.json.jackson.JacksonJsonpMapper());
            co.elastic.clients.elasticsearch.ElasticsearchClient client =
                    new co.elastic.clients.elasticsearch.ElasticsearchClient(transport);

            String indexName = "index_demo";

            try {
                // 1. 创建索引 + Mapping
                System.out.println("  【创建索引 + Mapping】");
                if (client.indices().exists(e -> e.index(indexName)).value()) {
                    client.indices().delete(d -> d.index(indexName));
                }

                client.indices().create(c -> c
                        .index(indexName)
                        .settings(s -> s.numberOfShards("1").numberOfReplicas("0"))
                        .mappings(m -> m
                                .properties("title", p -> p.text(t -> t.analyzer("standard")))
                                .properties("content", p -> p.text(t -> t.analyzer("standard")))
                                .properties("category", p -> p.keyword(k -> k))
                                .properties("views", p -> p.integer(i -> i))));
                System.out.println("    创建索引 " + indexName + "，1 分片 0 副本");

                // 2. 查看 Mapping
                System.out.println("\n  【查看 Mapping】");
                var mappingResp = client.indices().getMapping(g -> g.index(indexName));
                var properties = mappingResp.get(indexName).mappings().properties();
                for (var entry : properties.entrySet()) {
                    System.out.printf("    %s → %s%n", entry.getKey(), entry.getValue()._kind());
                }

                // 3. 写入文档并查看分词结果
                System.out.println("\n  【Analyze API — 查看分词结果】");
                var analyzeResp = client.indices().analyze(a -> a
                        .index(indexName)
                        .field("title")
                        .text("Java 并发编程实战"));
                System.out.print("    'Java 并发编程实战' → [");
                var tokens = analyzeResp.tokens();
                for (int i = 0; i < tokens.size(); i++) {
                    if (i > 0) System.out.print(", ");
                    System.out.print(tokens.get(i).token());
                }
                System.out.println("]");

                // 4. 索引设置查看
                System.out.println("\n  【查看索引设置】");
                var settingsResp = client.indices().getSettings(g -> g.index(indexName));
                var settings = settingsResp.get(indexName).settings().index();
                System.out.printf("    shards=%s, replicas=%s%n",
                        settings.numberOfShards(), settings.numberOfReplicas());

            } finally {
                cleanup(client, indexName);
                restClient.close();
            }
        }

        static void cleanup(co.elastic.clients.elasticsearch.ElasticsearchClient client,
                            String indexName) throws Exception {
            if (client.indices().exists(e -> e.index(indexName)).value()) {
                client.indices().delete(d -> d.index(indexName));
                System.out.println("\n  清理：已删除测试索引 " + indexName);
                System.out.println("  提示：如需保留数据，注释掉 cleanup() 调用即可");
            }
        }
    }
}
