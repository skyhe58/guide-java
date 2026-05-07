package com.example.es.query;

/**
 * Elasticsearch 查询 DSL 演示（混合模式）
 *
 * <p>Part A：用 Java Stream + Predicate 模拟 ES 查询原理（直接运行）
 * <p>Part B：用 ES Java Client 连接真实 Elasticsearch 执行查询 DSL
 *
 * <p>Part B 运行前启动依赖：
 * {@code docker compose -f docker/docker-compose.es.yml up -d elasticsearch}
 *
 * <p>本示例用 Java Stream 和 Predicate 模拟 ES 的核心查询类型：
 * <ul>
 *   <li>Match 查询 — 全文检索，分词后匹配</li>
 *   <li>Term 查询 — 精确匹配，不分词</li>
 *   <li>Range 查询 — 范围查询</li>
 *   <li>Bool 查询 — must/should/must_not/filter 组合</li>
 *   <li>Wildcard/Prefix 查询 — 通配符和前缀匹配</li>
 *   <li>Match vs Term 对比 — 理解分词的影响</li>
 * </ul>
 *
 * <p>如需连接真实 Elasticsearch，使用：
 * {@code docker compose -f docker/docker-compose.es.yml up -d elasticsearch}</p>
 *
 * <h3>ES 查询类型分类：</h3>
 * <pre>
 *  查询上下文（Query Context）— 计算相关性评分 _score
 *  ├── match         — 全文检索（分词后匹配）
 *  ├── multi_match   — 多字段全文检索
 *  ├── match_phrase  — 短语匹配（词序和位置都要匹配）
 *  └── bool          — 组合查询
 *
 *  过滤上下文（Filter Context）— 不计算评分，可缓存
 *  ├── term          — 精确匹配
 *  ├── terms         — 多值精确匹配（IN）
 *  ├── range         — 范围过滤
 *  └── exists        — 字段是否存在
 * </pre>
 */
public class QueryDemo {

    // ==================== 文档模型 ====================

    /** 模拟 ES 中的文章文档 */
    static class Article {
        final String id;
        final String title;
        final String content;
        final String author;
        final String category;
        final int views;
        final String publishDate;
        double score; // 相关性评分

        Article(String id, String title, String content, String author,
                String category, int views, String publishDate) {
            this.id = id;
            this.title = title;
            this.content = content;
            this.author = author;
            this.category = category;
            this.views = views;
            this.publishDate = publishDate;
        }

        @Override
        public String toString() {
            return String.format("{id=%s, title=%s, score=%.2f}", id, title, score);
        }
    }

    /** 准备测试数据 */
    static java.util.List<Article> prepareData() {
        return java.util.Arrays.asList(
                new Article("1", "Java 并发编程实战", "深入讲解 Java 多线程、锁机制、线程池的使用",
                        "张三", "技术", 5000, "2024-01-15"),
                new Article("2", "Spring Boot 入门指南", "从零开始学习 Spring Boot 框架开发 Web 应用",
                        "李四", "技术", 8000, "2024-02-20"),
                new Article("3", "MySQL 索引优化", "B+树索引原理、覆盖索引、最左前缀匹配详解",
                        "张三", "数据库", 3000, "2024-03-10"),
                new Article("4", "Redis 缓存设计", "缓存穿透、缓存击穿、缓存雪崩的解决方案",
                        "王五", "数据库", 6000, "2024-01-25"),
                new Article("5", "微服务架构设计", "Spring Cloud 微服务架构的设计与实践",
                        "李四", "架构", 10000, "2024-04-01"),
                new Article("6", "Java 虚拟机深入理解", "JVM 内存模型、垃圾回收、类加载机制",
                        "赵六", "技术", 4500, "2024-02-15"),
                new Article("7", "Elasticsearch 全文检索", "倒排索引原理、分词器、查询 DSL 详解",
                        "张三", "数据库", 7000, "2024-03-20"),
                new Article("8", "Docker 容器化部署", "Docker 镜像构建、容器编排、Kubernetes 入门",
                        "王五", "运维", 2000, "2024-04-10")
        );
    }

    // ==================== 简易分词器 ====================

    /** 模拟 ES 的中文分词（简化版：按字符和空格分词） */
    static java.util.Set<String> tokenize(String text) {
        java.util.Set<String> tokens = new java.util.HashSet<>();
        // 按空格和标点分词
        for (String word : text.split("[\\s,，。、；：！？()（）]+")) {
            if (!word.isEmpty()) {
                tokens.add(word.toLowerCase());
                // 模拟 bigram 分词：每两个字一组
                for (int i = 0; i < word.length() - 1; i++) {
                    tokens.add(word.substring(i, i + 2).toLowerCase());
                }
            }
        }
        return tokens;
    }

    /** 计算简单的 TF 相关性评分 */
    static double calculateScore(String text, String query) {
        java.util.Set<String> textTokens = tokenize(text);
        java.util.Set<String> queryTokens = tokenize(query);
        long matchCount = queryTokens.stream().filter(textTokens::contains).count();
        return queryTokens.isEmpty() ? 0 : (double) matchCount / queryTokens.size();
    }

    // ==================== 演示方法 ====================

    /** 演示1：Match 查询 — 全文检索 */
    static void demoMatchQuery() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示1：Match 查询 — 全文检索（分词后匹配）");
        System.out.println("═══════════════════════════════════════════════════");

        java.util.List<Article> articles = prepareData();

        // match 查询：对查询词分词，然后在倒排索引中查找
        String query = "Java 并发";
        System.out.printf("\n  GET /articles/_search%n");
        System.out.printf("  { \"query\": { \"match\": { \"title\": \"%s\" } } }%n", query);
        System.out.println("  分词结果: " + tokenize(query));

        java.util.List<Article> results = articles.stream()
                .peek(a -> a.score = calculateScore(a.title + " " + a.content, query))
                .filter(a -> a.score > 0)
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .collect(java.util.stream.Collectors.toList());

        System.out.println("  结果（按相关性排序）：");
        for (Article a : results) {
            System.out.printf("    _score=%.2f  %s — %s%n", a.score, a.title, a.author);
        }

        // match_phrase：短语匹配
        System.out.println("\n  【match_phrase】短语匹配 — 要求词序一致");
        System.out.println("  { \"query\": { \"match_phrase\": { \"content\": \"Spring Boot\" } } }");
        long phraseCount = articles.stream()
                .filter(a -> a.content.contains("Spring Boot") || a.title.contains("Spring Boot"))
                .count();
        System.out.printf("  匹配 %d 篇（内容中包含完整短语 \"Spring Boot\"）%n", phraseCount);
        System.out.println();
    }

    /** 演示2：Term 查询 — 精确匹配 */
    static void demoTermQuery() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示2：Term 查询 — 精确匹配（不分词）");
        System.out.println("═══════════════════════════════════════════════════");

        java.util.List<Article> articles = prepareData();

        // term 查询：精确匹配，不对查询词分词
        System.out.println("\n  { \"query\": { \"term\": { \"category\": \"技术\" } } }");
        java.util.List<Article> techArticles = articles.stream()
                .filter(a -> a.category.equals("技术"))
                .collect(java.util.stream.Collectors.toList());
        System.out.println("  结果：");
        for (Article a : techArticles) {
            System.out.printf("    %s — %s [%s]%n", a.title, a.author, a.category);
        }

        // terms 查询：多值匹配（类似 SQL IN）
        System.out.println("\n  { \"query\": { \"terms\": { \"author\": [\"张三\", \"李四\"] } } }");
        java.util.Set<String> authors = new java.util.HashSet<>(java.util.Arrays.asList("张三", "李四"));
        java.util.List<Article> byAuthors = articles.stream()
                .filter(a -> authors.contains(a.author))
                .collect(java.util.stream.Collectors.toList());
        System.out.println("  结果：");
        for (Article a : byAuthors) {
            System.out.printf("    %s — %s%n", a.title, a.author);
        }
        System.out.println();
    }

    /** 演示3：Range 查询 — 范围查询 */
    static void demoRangeQuery() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示3：Range 查询 — 范围查询");
        System.out.println("═══════════════════════════════════════════════════");

        java.util.List<Article> articles = prepareData();

        // 数值范围
        System.out.println("\n  { \"query\": { \"range\": { \"views\": { \"gte\": 5000, \"lte\": 10000 } } } }");
        java.util.List<Article> byViews = articles.stream()
                .filter(a -> a.views >= 5000 && a.views <= 10000)
                .sorted((a, b) -> Integer.compare(b.views, a.views))
                .collect(java.util.stream.Collectors.toList());
        System.out.println("  结果（浏览量 5000~10000）：");
        for (Article a : byViews) {
            System.out.printf("    %s — views=%d%n", a.title, a.views);
        }

        // 日期范围
        System.out.println("\n  { \"query\": { \"range\": { \"publishDate\": { \"gte\": \"2024-02-01\", \"lt\": \"2024-04-01\" } } } }");
        java.util.List<Article> byDate = articles.stream()
                .filter(a -> a.publishDate.compareTo("2024-02-01") >= 0
                        && a.publishDate.compareTo("2024-04-01") < 0)
                .collect(java.util.stream.Collectors.toList());
        System.out.println("  结果（2024-02 到 2024-03）：");
        for (Article a : byDate) {
            System.out.printf("    %s — %s%n", a.title, a.publishDate);
        }
        System.out.println();
    }

    /** 演示4：Bool 查询 — 组合查询 */
    static void demoBoolQuery() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示4：Bool 查询 — must/should/must_not/filter");
        System.out.println("═══════════════════════════════════════════════════");

        java.util.List<Article> articles = prepareData();

        // Bool 查询组合
        System.out.println("\n  复合查询：");
        System.out.println("  {");
        System.out.println("    \"query\": { \"bool\": {");
        System.out.println("      \"must\": [{ \"match\": { \"content\": \"Java\" } }],");
        System.out.println("      \"filter\": [{ \"range\": { \"views\": { \"gte\": 3000 } } }],");
        System.out.println("      \"must_not\": [{ \"term\": { \"category\": \"运维\" } }],");
        System.out.println("      \"should\": [{ \"term\": { \"author\": \"张三\" } }]");
        System.out.println("    }}");
        System.out.println("  }");

        java.util.List<Article> results = articles.stream()
                // must：必须匹配（影响评分）
                .filter(a -> a.content.contains("Java") || a.title.contains("Java"))
                // filter：必须匹配（不影响评分，可缓存）
                .filter(a -> a.views >= 3000)
                // must_not：必须不匹配
                .filter(a -> !a.category.equals("运维"))
                .peek(a -> {
                    a.score = calculateScore(a.title + " " + a.content, "Java");
                    // should：匹配则加分
                    if (a.author.equals("张三")) a.score += 0.5;
                })
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .collect(java.util.stream.Collectors.toList());

        System.out.println("\n  结果：");
        for (Article a : results) {
            System.out.printf("    _score=%.2f  %s — %s [%s] views=%d%n",
                    a.score, a.title, a.author, a.category, a.views);
        }

        // 解释 Bool 各子句的作用
        System.out.println("\n  Bool 子句说明：");
        System.out.println("    must     — 必须匹配，参与评分（AND 语义）");
        System.out.println("    filter   — 必须匹配，不参与评分，结果可缓存（性能更好）");
        System.out.println("    should   — 可选匹配，匹配则加分（OR 语义）");
        System.out.println("    must_not — 必须不匹配，不参与评分（NOT 语义）");
        System.out.println();
    }

    /** 演示5：Match vs Term 对比 */
    static void demoMatchVsTerm() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示5：Match vs Term — 理解分词的影响");
        System.out.println("═══════════════════════════════════════════════════");

        java.util.List<Article> articles = prepareData();

        // Match 查询："Spring Boot" 会被分词为 ["spring", "boot"]
        System.out.println("\n  【Match 查询】\"Spring Boot\" → 分词后匹配");
        System.out.println("  分词结果: " + tokenize("Spring Boot"));
        long matchCount = articles.stream()
                .filter(a -> {
                    java.util.Set<String> tokens = tokenize(a.title + " " + a.content);
                    return tokens.contains("spring") || tokens.contains("boot");
                })
                .peek(a -> System.out.printf("    ✓ %s%n", a.title))
                .count();
        System.out.printf("  匹配 %d 篇%n", matchCount);

        // Term 查询："Spring Boot" 不分词，精确匹配
        System.out.println("\n  【Term 查询】\"Spring Boot\" → 不分词，精确匹配");
        System.out.println("  注意：text 类型字段存储时会分词，term 查询不分词");
        System.out.println("  所以用 term 查询 text 字段通常匹配不到！");
        long termCount = articles.stream()
                .filter(a -> a.title.equals("Spring Boot"))
                .count();
        System.out.printf("  匹配 %d 篇（title 精确等于 \"Spring Boot\" 的文档）%n", termCount);

        System.out.println("\n  最佳实践：");
        System.out.println("    text 类型字段（需要分词）→ 用 match 查询");
        System.out.println("    keyword 类型字段（不分词）→ 用 term 查询");
        System.out.println("    数值/日期/布尔字段 → 用 term 或 range 查询");
        System.out.println();
    }

    /** 演示6：Wildcard 和 Prefix 查询 */
    static void demoWildcardAndPrefix() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示6：Wildcard 和 Prefix 查询");
        System.out.println("═══════════════════════════════════════════════════");

        java.util.List<Article> articles = prepareData();

        // Prefix 查询
        System.out.println("\n  【Prefix】{ \"prefix\": { \"title\": \"Java\" } }");
        articles.stream()
                .filter(a -> a.title.startsWith("Java"))
                .forEach(a -> System.out.printf("    %s%n", a.title));

        // Wildcard 查询
        System.out.println("\n  【Wildcard】{ \"wildcard\": { \"title\": \"*索引*\" } }");
        articles.stream()
                .filter(a -> a.title.contains("索引"))
                .forEach(a -> System.out.printf("    %s%n", a.title));

        System.out.println("\n  注意：wildcard 和 prefix 查询性能较差（需要扫描大量 term）");
        System.out.println("  建议：优先使用 match 查询 + 合适的分词器");
        System.out.println();
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  Elasticsearch 查询 DSL 演示（混合模式）               ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        // ===== Part A：纯内存模拟（直接运行） =====
        System.out.println("══════════ Part A：Predicate 模拟查询原理 ══════════");
        System.out.println();
        demoMatchQuery();
        demoTermQuery();
        demoRangeQuery();
        demoBoolQuery();
        demoMatchVsTerm();
        demoWildcardAndPrefix();

        // ===== Part B：连接真实 ES（需要 Docker 启动 ES） =====
        if (args.length > 0 && "real".equals(args[0])) {
            System.out.println("══════════ Part B：连接真实 Elasticsearch ══════════");
            System.out.println();
            RealEsQuery.run();
        } else {
            System.out.println("提示：运行 Part B（真实 ES 查询）请传入参数 'real'");
            System.out.println("  java -cp ... com.example.es.query.QueryDemo real");
        }
    }

    // ==================== Part B：真实 ES 查询 ====================

    /**
     * Part B：使用 Elasticsearch Java Client 连接真实 ES 执行查询。
     * 需要先启动 ES：docker compose -f docker/docker-compose.es.yml up -d elasticsearch
     */
    static class RealEsQuery {

        static void run() throws Exception {
            org.apache.http.HttpHost host = new org.apache.http.HttpHost("localhost", 9200, "http");
            org.elasticsearch.client.RestClient restClient =
                    org.elasticsearch.client.RestClient.builder(host).build();
            co.elastic.clients.transport.ElasticsearchTransport transport =
                    new co.elastic.clients.transport.rest_client.RestClientTransport(
                            restClient,
                            new co.elastic.clients.json.jackson.JacksonJsonpMapper());
            co.elastic.clients.elasticsearch.ElasticsearchClient client =
                    new co.elastic.clients.elasticsearch.ElasticsearchClient(transport);

            String indexName = "articles_demo";

            try {
                prepareTestData(client, indexName);
                demoRealMatchQuery(client, indexName);
                demoRealTermQuery(client, indexName);
                demoRealRangeQuery(client, indexName);
                demoRealBoolQuery(client, indexName);
            } finally {
                // 清理测试数据（可注释掉以保留数据，方便在 Kibana 中查看）
                // client.indices().delete(d -> d.index(indexName));
                // System.out.println("  清理：已删除测试索引 " + indexName);
                cleanup(client, indexName);
                restClient.close();
            }
        }

        /** 清理测试索引。如需保留数据，注释掉 cleanup() 调用即可 */
        static void cleanup(co.elastic.clients.elasticsearch.ElasticsearchClient client,
                            String indexName) throws Exception {
            client.indices().delete(d -> d.index(indexName));
            System.out.println("  清理：已删除测试索引 " + indexName);
            System.out.println("  提示：如需保留数据，注释掉 cleanup() 调用即可");
        }
        }

        /** 写入测试数据 */
        static void prepareTestData(co.elastic.clients.elasticsearch.ElasticsearchClient client,
                                    String indexName) throws Exception {
            if (client.indices().exists(e -> e.index(indexName)).value()) {
                client.indices().delete(d -> d.index(indexName));
            }

            // 创建索引并指定 mapping（title 用 text + keyword 双字段）
            client.indices().create(c -> c
                    .index(indexName)
                    .mappings(m -> m
                            .properties("title", p -> p.text(t -> t
                                    .analyzer("standard")
                                    .fields("keyword", f -> f.keyword(k -> k.ignoreAbove(256)))))
                            .properties("content", p -> p.text(t -> t.analyzer("standard")))
                            .properties("author", p -> p.keyword(k -> k))
                            .properties("category", p -> p.keyword(k -> k))
                            .properties("views", p -> p.integer(i -> i))
                            .properties("publishDate", p -> p.date(d -> d.format("yyyy-MM-dd")))));

            String[][] articles = {
                    {"Java 并发编程实战", "深入讲解 Java 多线程 锁机制 线程池", "张三", "技术", "5000", "2024-01-15"},
                    {"Spring Boot 入门指南", "从零开始学习 Spring Boot 框架开发", "李四", "技术", "8000", "2024-02-20"},
                    {"MySQL 索引优化", "B+树索引原理 覆盖索引 最左前缀匹配", "张三", "数据库", "3000", "2024-03-10"},
                    {"Redis 缓存设计", "缓存穿透 缓存击穿 缓存雪崩解决方案", "王五", "数据库", "6000", "2024-01-25"},
                    {"微服务架构设计", "Spring Cloud 微服务架构设计与实践", "李四", "架构", "10000", "2024-04-01"},
                    {"Elasticsearch 全文检索", "倒排索引原理 分词器 查询DSL详解", "张三", "数据库", "7000", "2024-03-20"},
            };

            co.elastic.clients.elasticsearch.core.BulkRequest.Builder bulk =
                    new co.elastic.clients.elasticsearch.core.BulkRequest.Builder();
            for (int i = 0; i < articles.length; i++) {
                String[] a = articles[i];
                java.util.Map<String, Object> doc = new java.util.LinkedHashMap<>();
                doc.put("title", a[0]);
                doc.put("content", a[1]);
                doc.put("author", a[2]);
                doc.put("category", a[3]);
                doc.put("views", Integer.parseInt(a[4]));
                doc.put("publishDate", a[5]);
                final int id = i + 1;
                bulk.operations(op -> op.index(idx -> idx.index(indexName).id(String.valueOf(id)).document(doc)));
            }
            client.bulk(bulk.build());
            client.indices().refresh(r -> r.index(indexName));
            System.out.println("  准备数据：写入 " + articles.length + " 篇文章到 " + indexName);
            System.out.println();
        }

        /** Match 查询：全文检索 */
        static void demoRealMatchQuery(co.elastic.clients.elasticsearch.ElasticsearchClient client,
                                       String indexName) throws Exception {
            System.out.println("  【真实 ES — Match 查询】搜索 'Java 并发'");

            var response = client.search(s -> s
                            .index(indexName)
                            .query(q -> q.match(m -> m.field("title").query("Java 并发"))),
                    java.util.Map.class);

            System.out.printf("    命中 %d 篇：%n", response.hits().total().value());
            for (var hit : response.hits().hits()) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> source = (java.util.Map<String, Object>) hit.source();
                System.out.printf("    _score=%.4f  %s%n", hit.score(), source.get("title"));
            }
            System.out.println();
        }

        /** Term 查询：精确匹配 keyword 字段 */
        static void demoRealTermQuery(co.elastic.clients.elasticsearch.ElasticsearchClient client,
                                      String indexName) throws Exception {
            System.out.println("  【真实 ES — Term 查询】category = '数据库'");

            var response = client.search(s -> s
                            .index(indexName)
                            .query(q -> q.term(t -> t.field("category").value("数据库"))),
                    java.util.Map.class);

            System.out.printf("    命中 %d 篇：%n", response.hits().total().value());
            for (var hit : response.hits().hits()) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> source = (java.util.Map<String, Object>) hit.source();
                System.out.printf("    %s — %s%n", source.get("title"), source.get("author"));
            }
            System.out.println();
        }

        /** Range 查询：浏览量范围 */
        static void demoRealRangeQuery(co.elastic.clients.elasticsearch.ElasticsearchClient client,
                                       String indexName) throws Exception {
            System.out.println("  【真实 ES — Range 查询】views >= 5000 AND views <= 10000");

            var response = client.search(s -> s
                            .index(indexName)
                            .query(q -> q.range(r -> r
                                    .number(n -> n.field("views").gte(5000.0).lte(10000.0)))),
                    java.util.Map.class);

            System.out.printf("    命中 %d 篇：%n", response.hits().total().value());
            for (var hit : response.hits().hits()) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> source = (java.util.Map<String, Object>) hit.source();
                System.out.printf("    %s — views=%s%n", source.get("title"), source.get("views"));
            }
            System.out.println();
        }

        /** Bool 查询：组合查询 */
        static void demoRealBoolQuery(co.elastic.clients.elasticsearch.ElasticsearchClient client,
                                      String indexName) throws Exception {
            System.out.println("  【真实 ES — Bool 查询】must: match 'Java' + filter: views>=3000 + must_not: category='架构'");

            var response = client.search(s -> s
                            .index(indexName)
                            .query(q -> q.bool(b -> b
                                    .must(m -> m.match(mt -> mt.field("content").query("Java")))
                                    .filter(f -> f.range(r -> r.number(n -> n.field("views").gte(3000.0))))
                                    .mustNot(mn -> mn.term(t -> t.field("category").value("架构"))))),
                    java.util.Map.class);

            System.out.printf("    命中 %d 篇：%n", response.hits().total().value());
            for (var hit : response.hits().hits()) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> source = (java.util.Map<String, Object>) hit.source();
                System.out.printf("    _score=%.4f  %s [%s] views=%s%n",
                        hit.score(), source.get("title"), source.get("category"), source.get("views"));
            }
            System.out.println();
        }
    }
}
