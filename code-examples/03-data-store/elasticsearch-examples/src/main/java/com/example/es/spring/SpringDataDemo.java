package com.example.es.spring;

/**
 * Spring Data Elasticsearch 演示（混合模式）
 *
 * <p>Part A：用内存 Map 模拟 Repository 模式（直接运行）
 * <ul>
 *   <li>Repository 接口模式：CRUD + 自定义查询方法</li>
 *   <li>分页与排序</li>
 *   <li>方法名派生查询（findByXxx）</li>
 * </ul>
 *
 * <p>Part B：用 ES Java Client 模拟 Spring Data ES 的核心行为
 *
 * <p>Part B 运行前启动依赖：
 * {@code docker compose -f docker/docker-compose.es.yml up -d elasticsearch}
 *
 * <h3>Spring Data ES 核心概念：</h3>
 * <pre>
 *  @Document(indexName = "articles")   ← 实体类映射到 ES 索引
 *  public class Article {
 *      @Id private String id;
 *      @Field(type = Text) private String title;
 *      @Field(type = Keyword) private String author;
 *  }
 *
 *  public interface ArticleRepository extends ElasticsearchRepository&lt;Article, String&gt; {
 *      List&lt;Article&gt; findByAuthor(String author);           ← 方法名派生查询
 *      List&lt;Article&gt; findByTitleContaining(String keyword);  ← 包含查询
 *      Page&lt;Article&gt; findByCategory(String cat, Pageable p); ← 分页查询
 *  }
 * </pre>
 */
public class SpringDataDemo {

    // ==================== 实体模型 ====================

    /** 模拟 @Document 注解的实体类 */
    static class Article {
        String id;
        String title;
        String author;
        String category;
        int views;

        Article(String id, String title, String author, String category, int views) {
            this.id = id; this.title = title; this.author = author;
            this.category = category; this.views = views;
        }

        @Override
        public String toString() {
            return String.format("{id=%s, title=%s, author=%s, category=%s, views=%d}",
                    id, title, author, category, views);
        }
    }

    /** 分页结果 */
    static class Page<T> {
        final java.util.List<T> content;
        final int pageNumber;
        final int pageSize;
        final long totalElements;
        final int totalPages;

        Page(java.util.List<T> content, int pageNumber, int pageSize, long totalElements) {
            this.content = content;
            this.pageNumber = pageNumber;
            this.pageSize = pageSize;
            this.totalElements = totalElements;
            this.totalPages = (int) Math.ceil((double) totalElements / pageSize);
        }
    }

    // ==================== Part A：模拟 Repository ====================

    /**
     * 模拟 ElasticsearchRepository 的核心功能。
     * Spring Data ES 的 Repository 底层就是调用 ES RestClient，
     * 这里用 Map 模拟存储，展示 Repository 模式的设计思想。
     */
    static class SimulatedArticleRepository {
        private final java.util.Map<String, Article> store = new java.util.LinkedHashMap<>();
        private int idCounter = 0;

        // ===== CRUD 基本操作 =====

        /** save：保存或更新文档（对应 IndexRequest） */
        Article save(Article article) {
            if (article.id == null) {
                article.id = String.valueOf(++idCounter);
            }
            store.put(article.id, article);
            return article;
        }

        /** findById：根据 ID 查找（对应 GetRequest） */
        java.util.Optional<Article> findById(String id) {
            return java.util.Optional.ofNullable(store.get(id));
        }

        /** findAll：查询所有文档 */
        java.util.List<Article> findAll() {
            return new java.util.ArrayList<>(store.values());
        }

        /** deleteById：根据 ID 删除（对应 DeleteRequest） */
        void deleteById(String id) {
            store.remove(id);
        }

        /** count：文档总数 */
        long count() {
            return store.size();
        }

        // ===== 方法名派生查询（Spring Data 的核心特性） =====

        /** findByAuthor → 等价 ES: {"query": {"term": {"author": "xxx"}}} */
        java.util.List<Article> findByAuthor(String author) {
            return store.values().stream()
                    .filter(a -> a.author.equals(author))
                    .collect(java.util.stream.Collectors.toList());
        }

        /** findByTitleContaining → 等价 ES: {"query": {"match": {"title": "xxx"}}} */
        java.util.List<Article> findByTitleContaining(String keyword) {
            return store.values().stream()
                    .filter(a -> a.title.contains(keyword))
                    .collect(java.util.stream.Collectors.toList());
        }

        /** findByCategory + 分页 → 等价 ES: {"query": ..., "from": x, "size": y} */
        Page<Article> findByCategory(String category, int pageNumber, int pageSize) {
            java.util.List<Article> filtered = store.values().stream()
                    .filter(a -> a.category.equals(category))
                    .collect(java.util.stream.Collectors.toList());

            long total = filtered.size();
            int from = pageNumber * pageSize;
            int to = Math.min(from + pageSize, filtered.size());
            java.util.List<Article> pageContent = from < filtered.size()
                    ? filtered.subList(from, to)
                    : java.util.Collections.emptyList();

            return new Page<>(pageContent, pageNumber, pageSize, total);
        }

        /** findByViewsGreaterThan + 排序 */
        java.util.List<Article> findByViewsGreaterThanOrderByViewsDesc(int minViews) {
            return store.values().stream()
                    .filter(a -> a.views > minViews)
                    .sorted((a, b) -> Integer.compare(b.views, a.views))
                    .collect(java.util.stream.Collectors.toList());
        }

        /** findByAuthorAndCategory → 组合条件 */
        java.util.List<Article> findByAuthorAndCategory(String author, String category) {
            return store.values().stream()
                    .filter(a -> a.author.equals(author) && a.category.equals(category))
                    .collect(java.util.stream.Collectors.toList());
        }
    }

    // ==================== Part A 演示方法 ====================

    /** 演示1：CRUD 基本操作 */
    static void demoCrud() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示1：Repository CRUD 基本操作");
        System.out.println("═══════════════════════════════════════════════════");

        SimulatedArticleRepository repo = new SimulatedArticleRepository();

        // Save
        System.out.println("\n  【Save】保存文档：");
        repo.save(new Article("1", "Java 并发编程", "张三", "技术", 5000));
        repo.save(new Article("2", "Spring Boot 入门", "李四", "技术", 8000));
        repo.save(new Article("3", "MySQL 索引优化", "张三", "数据库", 3000));
        repo.save(new Article("4", "Redis 缓存设计", "王五", "数据库", 6000));
        repo.save(new Article("5", "微服务架构", "李四", "架构", 10000));
        System.out.printf("    保存 %d 篇文章%n", repo.count());

        // FindById
        System.out.println("\n  【FindById】根据 ID 查找：");
        repo.findById("1").ifPresent(a -> System.out.println("    " + a));
        System.out.println("    findById('99') → " + (repo.findById("99").isPresent() ? "found" : "not found"));

        // Update（save 同一个 ID = 更新）
        System.out.println("\n  【Update】更新文档（save 同一个 ID）：");
        Article updated = new Article("1", "Java 并发编程实战", "张三", "技术", 5500);
        repo.save(updated);
        repo.findById("1").ifPresent(a -> System.out.println("    更新后: " + a));

        // Delete
        System.out.println("\n  【Delete】删除文档：");
        repo.deleteById("5");
        System.out.printf("    删除 id=5 后，剩余 %d 篇%n", repo.count());
        System.out.println();
    }

    /** 演示2：方法名派生查询 */
    static void demoDerivedQuery() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示2：方法名派生查询");
        System.out.println("═══════════════════════════════════════════════════");

        SimulatedArticleRepository repo = prepareRepo();

        // findByAuthor
        System.out.println("\n  【findByAuthor('张三')】");
        for (Article a : repo.findByAuthor("张三")) {
            System.out.println("    " + a);
        }

        // findByTitleContaining
        System.out.println("\n  【findByTitleContaining('Java')】");
        for (Article a : repo.findByTitleContaining("Java")) {
            System.out.println("    " + a);
        }

        // findByAuthorAndCategory
        System.out.println("\n  【findByAuthorAndCategory('张三', '技术')】");
        for (Article a : repo.findByAuthorAndCategory("张三", "技术")) {
            System.out.println("    " + a);
        }

        // findByViewsGreaterThan + 排序
        System.out.println("\n  【findByViewsGreaterThanOrderByViewsDesc(5000)】");
        for (Article a : repo.findByViewsGreaterThanOrderByViewsDesc(5000)) {
            System.out.printf("    %s (views=%d)%n", a.title, a.views);
        }

        System.out.println("\n  方法名命名规则：");
        System.out.println("    findBy{Field}           → term 精确匹配");
        System.out.println("    findBy{Field}Containing  → match 全文检索");
        System.out.println("    findBy{Field}Between     → range 范围查询");
        System.out.println("    findBy{A}And{B}          → bool must 组合");
        System.out.println("    findBy{A}Or{B}           → bool should 组合");
        System.out.println("    OrderBy{Field}Desc       → sort 排序");
        System.out.println();
    }

    /** 演示3：分页查询 */
    static void demoPagination() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示3：分页查询");
        System.out.println("═══════════════════════════════════════════════════");

        SimulatedArticleRepository repo = new SimulatedArticleRepository();
        // 插入 12 篇技术文章
        for (int i = 1; i <= 12; i++) {
            repo.save(new Article(null, "技术文章 " + i, "作者" + (i % 3), "技术", i * 100));
        }

        System.out.println("\n  共 " + repo.count() + " 篇文章，按 category='技术' 分页查询（每页 5 条）：");

        for (int page = 0; page < 3; page++) {
            Page<Article> result = repo.findByCategory("技术", page, 5);
            System.out.printf("\n  第 %d 页（%d/%d 页，共 %d 条）：%n",
                    page + 1, page + 1, result.totalPages, result.totalElements);
            for (Article a : result.content) {
                System.out.println("    " + a);
            }
        }
        System.out.println();
    }

    // ==================== 辅助方法 ====================

    static SimulatedArticleRepository prepareRepo() {
        SimulatedArticleRepository repo = new SimulatedArticleRepository();
        repo.save(new Article("1", "Java 并发编程", "张三", "技术", 5000));
        repo.save(new Article("2", "Spring Boot 入门", "李四", "技术", 8000));
        repo.save(new Article("3", "MySQL 索引优化", "张三", "数据库", 3000));
        repo.save(new Article("4", "Redis 缓存设计", "王五", "数据库", 6000));
        repo.save(new Article("5", "微服务架构", "李四", "架构", 10000));
        repo.save(new Article("6", "Java 虚拟机", "赵六", "技术", 4500));
        return repo;
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  Spring Data Elasticsearch 演示（混合模式）            ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        // ===== Part A：内存模拟 Repository =====
        System.out.println("══════════ Part A：模拟 Repository 模式 ══════════");
        System.out.println();
        demoCrud();
        demoDerivedQuery();
        demoPagination();

        // ===== Part B：连接真实 ES =====
        if (args.length > 0 && "real".equals(args[0])) {
            System.out.println("══════════ Part B：ES Java Client 模拟 Repository ══════════");
            System.out.println();
            RealEsRepository.run();
        } else {
            System.out.println("提示：运行 Part B（真实 ES Repository）请传入参数 'real'");
        }
    }

    // ==================== Part B：真实 ES Repository 模拟 ====================

    static class RealEsRepository {

        static void run() throws Exception {
            org.apache.http.HttpHost host = new org.apache.http.HttpHost("localhost", 9200, "http");
            org.elasticsearch.client.RestClient restClient =
                    org.elasticsearch.client.RestClient.builder(host).build();
            co.elastic.clients.transport.ElasticsearchTransport transport =
                    new co.elastic.clients.transport.rest_client.RestClientTransport(
                            restClient, new co.elastic.clients.json.jackson.JacksonJsonpMapper());
            co.elastic.clients.elasticsearch.ElasticsearchClient client =
                    new co.elastic.clients.elasticsearch.ElasticsearchClient(transport);

            String indexName = "spring_data_demo";

            try {
                // 准备索引和数据
                if (client.indices().exists(e -> e.index(indexName)).value()) {
                    client.indices().delete(d -> d.index(indexName));
                }

                client.indices().create(c -> c
                        .index(indexName)
                        .mappings(m -> m
                                .properties("title", p -> p.text(t -> t.analyzer("standard")
                                        .fields("keyword", f -> f.keyword(k -> k.ignoreAbove(256)))))
                                .properties("author", p -> p.keyword(k -> k))
                                .properties("category", p -> p.keyword(k -> k))
                                .properties("views", p -> p.integer(i -> i))));

                // 批量写入
                String[][] articles = {
                        {"Java 并发编程", "张三", "技术", "5000"},
                        {"Spring Boot 入门", "李四", "技术", "8000"},
                        {"MySQL 索引优化", "张三", "数据库", "3000"},
                        {"Redis 缓存设计", "王五", "数据库", "6000"},
                        {"微服务架构", "李四", "架构", "10000"},
                        {"Java 虚拟机", "赵六", "技术", "4500"},
                };

                co.elastic.clients.elasticsearch.core.BulkRequest.Builder bulk =
                        new co.elastic.clients.elasticsearch.core.BulkRequest.Builder();
                for (int i = 0; i < articles.length; i++) {
                    String[] a = articles[i];
                    java.util.Map<String, Object> doc = new java.util.LinkedHashMap<>();
                    doc.put("title", a[0]); doc.put("author", a[1]);
                    doc.put("category", a[2]); doc.put("views", Integer.parseInt(a[3]));
                    final int id = i + 1;
                    bulk.operations(op -> op.index(idx -> idx.index(indexName).id(String.valueOf(id)).document(doc)));
                }
                client.bulk(bulk.build());
                client.indices().refresh(r -> r.index(indexName));
                System.out.println("  准备数据：写入 " + articles.length + " 篇文章");

                // 1. findByAuthor（term 查询）
                System.out.println("\n  【findByAuthor('张三')】→ term 查询");
                var resp1 = client.search(s -> s
                                .index(indexName)
                                .query(q -> q.term(t -> t.field("author").value("张三"))),
                        java.util.Map.class);
                for (var hit : resp1.hits().hits()) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> src = (java.util.Map<String, Object>) hit.source();
                    System.out.printf("    %s — %s%n", src.get("title"), src.get("author"));
                }

                // 2. findByTitleContaining（match 查询）
                System.out.println("\n  【findByTitleContaining('Java')】→ match 查询");
                var resp2 = client.search(s -> s
                                .index(indexName)
                                .query(q -> q.match(m -> m.field("title").query("Java"))),
                        java.util.Map.class);
                for (var hit : resp2.hits().hits()) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> src = (java.util.Map<String, Object>) hit.source();
                    System.out.printf("    %s (score=%.4f)%n", src.get("title"), hit.score());
                }

                // 3. 分页查询（from + size）
                System.out.println("\n  【分页查询】category='技术', page=0, size=2");
                var resp3 = client.search(s -> s
                                .index(indexName)
                                .query(q -> q.term(t -> t.field("category").value("技术")))
                                .from(0).size(2)
                                .sort(so -> so.field(f -> f.field("views").order(
                                        co.elastic.clients.elasticsearch._types.SortOrder.Desc))),
                        java.util.Map.class);
                System.out.printf("    总命中: %d, 当前页: %d 条%n",
                        resp3.hits().total().value(), resp3.hits().hits().size());
                for (var hit : resp3.hits().hits()) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> src = (java.util.Map<String, Object>) hit.source();
                    System.out.printf("    %s (views=%s)%n", src.get("title"), src.get("views"));
                }

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
