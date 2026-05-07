package com.example.es.aggregation;

/**
 * Elasticsearch 聚合操作演示（混合模式）
 *
 * <p>Part A：用 Java Stream API 模拟 ES 聚合原理（直接运行）
 * <p>Part B：用 ES Java Client 连接真实 Elasticsearch 执行聚合查询
 *
 * <p>Part B 运行前启动依赖：
 * {@code docker compose -f docker/docker-compose.es.yml up -d elasticsearch}
 *
 * <p>本示例用 Java Stream 模拟 ES 的核心聚合操作：
 * <ul>
 *   <li>Terms 聚合 — 按字段分组统计（类似 SQL GROUP BY）</li>
 *   <li>Avg/Sum/Min/Max 聚合 — 数值统计</li>
 *   <li>Range 聚合 — 按范围分桶</li>
 *   <li>Histogram 聚合 — 按固定间隔分桶</li>
 *   <li>嵌套聚合 — 在分组内再做统计</li>
 *   <li>Top Hits 聚合 — 每组取 Top N</li>
 * </ul>
 *
 * <p>如需连接真实 Elasticsearch，使用：
 * {@code docker compose -f docker/docker-compose.es.yml up -d elasticsearch}</p>
 *
 * <h3>ES 聚合类型：</h3>
 * <pre>
 *  聚合（Aggregation）
 *  ├── 桶聚合（Bucket）— 分组，类似 GROUP BY
 *  │   ├── terms      — 按字段值分组
 *  │   ├── range      — 按范围分桶
 *  │   ├── histogram  — 按固定间隔分桶
 *  │   └── date_histogram — 按时间间隔分桶
 *  ├── 指标聚合（Metric）— 计算统计值
 *  │   ├── avg / sum / min / max / count
 *  │   ├── stats      — 一次返回所有统计值
 *  │   └── cardinality — 去重计数（类似 COUNT DISTINCT）
 *  └── 管道聚合（Pipeline）— 对其他聚合结果再聚合
 *      ├── avg_bucket  — 对桶的某个指标求平均
 *      └── max_bucket  — 找出指标最大的桶
 * </pre>
 */
public class AggregationDemo {

    // ==================== 文档模型 ====================

    /**
     * 模拟 ES 中的商品文档
     */
    static class Product {
        final String id;
        final String name;
        final String category;  // 类目
        final String brand;     // 品牌
        final double price;     // 价格
        final int sales;        // 销量
        final String date;      // 上架日期

        Product(String id, String name, String category, String brand,
                double price, int sales, String date) {
            this.id = id;
            this.name = name;
            this.category = category;
            this.brand = brand;
            this.price = price;
            this.sales = sales;
            this.date = date;
        }
    }

    /**
     * 准备测试数据
     */
    static java.util.List<Product> prepareData() {
        return java.util.Arrays.asList(
                new Product("1", "iPhone 15", "手机", "Apple", 5999, 1200, "2024-01"),
                new Product("2", "iPhone 15 Pro", "手机", "Apple", 8999, 800, "2024-01"),
                new Product("3", "Mate 60", "手机", "华为", 4999, 1500, "2024-02"),
                new Product("4", "Mate 60 Pro", "手机", "华为", 6999, 900, "2024-02"),
                new Product("5", "小米 14", "手机", "小米", 3999, 2000, "2024-01"),
                new Product("6", "MacBook Pro", "电脑", "Apple", 14999, 500, "2024-03"),
                new Product("7", "ThinkPad X1", "电脑", "联想", 9999, 600, "2024-02"),
                new Product("8", "MateBook X", "电脑", "华为", 7999, 400, "2024-03"),
                new Product("9", "AirPods Pro", "耳机", "Apple", 1799, 3000, "2024-01"),
                new Product("10", "FreeBuds Pro", "耳机", "华为", 999, 2500, "2024-02"),
                new Product("11", "Redmi Buds", "耳机", "小米", 199, 5000, "2024-03"),
                new Product("12", "iPad Air", "平板", "Apple", 4799, 700, "2024-01")
        );
    }

    // ==================== 演示方法 ====================

    /**
     * 演示1：Terms 聚合 — 按字段分组统计
     */
    static void demoTermsAggregation() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示1：Terms 聚合 — 按字段分组统计");
        System.out.println("═══════════════════════════════════════════════════");

        java.util.List<Product> products = prepareData();

        // 对应 ES 查询：
        // GET /products/_search
        // { "aggs": { "by_category": { "terms": { "field": "category" } } } }
        System.out.println("\n  【按类目分组】等价 ES: terms agg on category");
        java.util.Map<String, Long> byCategory = products.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        p -> p.category, java.util.stream.Collectors.counting()));
        byCategory.entrySet().stream()
                .sorted(java.util.Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(e -> System.out.printf("    %s: %d 件%n", e.getKey(), e.getValue()));

        // 按品牌分组
        System.out.println("\n  【按品牌分组】等价 ES: terms agg on brand");
        java.util.Map<String, Long> byBrand = products.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        p -> p.brand, java.util.stream.Collectors.counting()));
        byBrand.entrySet().stream()
                .sorted(java.util.Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(e -> System.out.printf("    %s: %d 件%n", e.getKey(), e.getValue()));
        System.out.println();
    }

    /**
     * 演示2：Metric 聚合 — 数值统计
     */
    static void demoMetricAggregation() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示2：Metric 聚合 — 数值统计");
        System.out.println("═══════════════════════════════════════════════════");

        java.util.List<Product> products = prepareData();

        // stats 聚合：一次返回 count/min/max/avg/sum
        java.util.DoubleSummaryStatistics priceStats = products.stream()
                .mapToDouble(p -> p.price)
                .summaryStatistics();

        System.out.println("\n  【价格统计】等价 ES: stats agg on price");
        System.out.printf("    count: %d%n", priceStats.getCount());
        System.out.printf("    min:   %.0f%n", priceStats.getMin());
        System.out.printf("    max:   %.0f%n", priceStats.getMax());
        System.out.printf("    avg:   %.2f%n", priceStats.getAverage());
        System.out.printf("    sum:   %.0f%n", priceStats.getSum());

        // cardinality 聚合：去重计数
        long brandCount = products.stream()
                .map(p -> p.brand)
                .distinct()
                .count();
        System.out.printf("\n  【品牌去重计数】等价 ES: cardinality agg on brand%n");
        System.out.printf("    distinct brands: %d%n", brandCount);

        // 总销量
        int totalSales = products.stream().mapToInt(p -> p.sales).sum();
        System.out.printf("\n  【总销量】sum agg on sales: %d%n", totalSales);
        System.out.println();
    }

    /**
     * 演示3：Range 聚合 — 按范围分桶
     */
    static void demoRangeAggregation() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示3：Range 聚合 — 按价格范围分桶");
        System.out.println("═══════════════════════════════════════════════════");

        java.util.List<Product> products = prepareData();

        // 对应 ES：
        // { "aggs": { "price_ranges": { "range": { "field": "price",
        //   "ranges": [ {"to": 1000}, {"from": 1000, "to": 5000},
        //               {"from": 5000, "to": 10000}, {"from": 10000} ] } } } }
        System.out.println("\n  价格区间分桶：");
        int[][] ranges = {{0, 1000}, {1000, 5000}, {5000, 10000}, {10000, Integer.MAX_VALUE}};
        String[] labels = {"0-1000（低价）", "1000-5000（中价）", "5000-10000（高价）", "10000+（奢侈）"};

        for (int i = 0; i < ranges.length; i++) {
            final int from = ranges[i][0], to = ranges[i][1];
            long count = products.stream()
                    .filter(p -> p.price >= from && p.price < to)
                    .count();
            double avgPrice = products.stream()
                    .filter(p -> p.price >= from && p.price < to)
                    .mapToDouble(p -> p.price)
                    .average().orElse(0);
            System.out.printf("    %-20s: %d 件, 均价 %.0f%n", labels[i], count, avgPrice);
        }
        System.out.println();
    }

    /**
     * 演示4：Histogram 聚合 — 按固定间隔分桶
     */
    static void demoHistogramAggregation() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示4：Histogram 聚合 — 按固定间隔分桶");
        System.out.println("═══════════════════════════════════════════════════");

        java.util.List<Product> products = prepareData();

        // 按 2000 元间隔分桶
        int interval = 2000;
        System.out.printf("\n  价格直方图（间隔 %d 元）：%n", interval);

        java.util.Map<Integer, java.util.List<Product>> histogram = products.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        p -> ((int) p.price / interval) * interval));

        histogram.entrySet().stream()
                .sorted(java.util.Map.Entry.comparingByKey())
                .forEach(e -> {
                    int bucketStart = e.getKey();
                    int count = e.getValue().size();
                    String bar = "█".repeat(count * 3);
                    System.out.printf("    %5d-%5d: %d %s%n",
                            bucketStart, bucketStart + interval, count, bar);
                });

        // 按月份分桶（模拟 date_histogram）
        System.out.println("\n  按月份分桶（模拟 date_histogram）：");
        java.util.Map<String, Long> byMonth = products.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        p -> p.date, java.util.stream.Collectors.counting()));
        byMonth.entrySet().stream()
                .sorted(java.util.Map.Entry.comparingByKey())
                .forEach(e -> {
                    String bar = "█".repeat((int) (e.getValue() * 3));
                    System.out.printf("    %s: %d %s%n", e.getKey(), e.getValue(), bar);
                });
        System.out.println();
    }

    /**
     * 演示5：嵌套聚合 — 分组内再统计
     */
    static void demoNestedAggregation() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示5：嵌套聚合 — 每个品牌的平均价格和总销量");
        System.out.println("═══════════════════════════════════════════════════");

        java.util.List<Product> products = prepareData();

        // 对应 ES：
        // { "aggs": { "by_brand": { "terms": { "field": "brand" },
        //   "aggs": { "avg_price": { "avg": { "field": "price" } },
        //             "total_sales": { "sum": { "field": "sales" } } } } } }
        System.out.println("\n  等价 ES: terms(brand) → avg(price) + sum(sales)");

        java.util.Map<String, java.util.List<Product>> byBrand = products.stream()
                .collect(java.util.stream.Collectors.groupingBy(p -> p.brand));

        System.out.printf("  %-8s %-6s %-12s %-10s %-20s%n",
                "品牌", "数量", "平均价格", "总销量", "商品列表");
        System.out.println("  " + "─".repeat(60));

        byBrand.entrySet().stream()
                .sorted((a, b) -> {
                    double avgA = a.getValue().stream().mapToDouble(p -> p.price).average().orElse(0);
                    double avgB = b.getValue().stream().mapToDouble(p -> p.price).average().orElse(0);
                    return Double.compare(avgB, avgA);
                })
                .forEach(e -> {
                    java.util.List<Product> items = e.getValue();
                    double avgPrice = items.stream().mapToDouble(p -> p.price).average().orElse(0);
                    int totalSales = items.stream().mapToInt(p -> p.sales).sum();
                    String names = items.stream().map(p -> p.name)
                            .collect(java.util.stream.Collectors.joining(", "));
                    System.out.printf("  %-8s %-6d %-12.0f %-10d %-20s%n",
                            e.getKey(), items.size(), avgPrice, totalSales, names);
                });

        // 每个类目中价格最高的商品（Top Hits）
        System.out.println("\n  【Top Hits】每个类目中价格最高的商品：");
        java.util.Map<String, java.util.List<Product>> byCategory = products.stream()
                .collect(java.util.stream.Collectors.groupingBy(p -> p.category));

        byCategory.forEach((category, items) -> {
            Product top = items.stream()
                    .max(java.util.Comparator.comparingDouble(p -> p.price))
                    .orElse(null);
            if (top != null) {
                System.out.printf("    %s → %s (%.0f 元)%n", category, top.name, top.price);
            }
        });
        System.out.println();
    }

    /**
     * 演示6：Pipeline 聚合 — 对聚合结果再聚合
     */
    static void demoPipelineAggregation() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示6：Pipeline 聚合 — 对聚合结果再聚合");
        System.out.println("═══════════════════════════════════════════════════");

        java.util.List<Product> products = prepareData();

        // 先按品牌分组求平均价格，再找出平均价格最高的品牌
        // 对应 ES: max_bucket / avg_bucket
        System.out.println("\n  步骤1：按品牌分组求平均价格");
        java.util.Map<String, Double> brandAvgPrice = products.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        p -> p.brand,
                        java.util.stream.Collectors.averagingDouble(p -> p.price)));

        brandAvgPrice.forEach((brand, avg) ->
                System.out.printf("    %s: 平均 %.0f 元%n", brand, avg));

        // max_bucket：找出平均价格最高的品牌
        java.util.Map.Entry<String, Double> maxBrand = brandAvgPrice.entrySet().stream()
                .max(java.util.Map.Entry.comparingByValue())
                .orElse(null);
        System.out.printf("\n  步骤2 (max_bucket): 平均价格最高的品牌 → %s (%.0f 元)%n",
                maxBrand.getKey(), maxBrand.getValue());

        // avg_bucket：所有品牌平均价格的平均值
        double avgOfAvg = brandAvgPrice.values().stream()
                .mapToDouble(Double::doubleValue)
                .average().orElse(0);
        System.out.printf("  步骤2 (avg_bucket): 各品牌平均价格的均值 → %.0f 元%n", avgOfAvg);
        System.out.println();
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  Elasticsearch 聚合操作演示（混合模式）                ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        // ===== Part A：纯内存模拟（直接运行） =====
        System.out.println("══════════ Part A：Stream API 模拟聚合原理 ══════════");
        System.out.println();
        demoTermsAggregation();
        demoMetricAggregation();
        demoRangeAggregation();
        demoHistogramAggregation();
        demoNestedAggregation();
        demoPipelineAggregation();

        // ===== Part B：连接真实 ES（需要 Docker 启动 ES） =====
        // 启动命令：docker compose -f docker/docker-compose.es.yml up -d elasticsearch
        // 默认地址：http://localhost:9200
        if (args.length > 0 && "real".equals(args[0])) {
            System.out.println("══════════ Part B：连接真实 Elasticsearch ══════════");
            System.out.println();
            RealEsAggregation.run();
        } else {
            System.out.println("提示：运行 Part B（真实 ES 聚合）请传入参数 'real'");
            System.out.println("  java -cp ... com.example.es.aggregation.AggregationDemo real");
        }
    }

    // ==================== Part B：真实 ES 聚合 ====================

    /**
     * Part B：使用 Elasticsearch Java Client 连接真实 ES 执行聚合查询。
     * 需要先启动 ES：docker compose -f docker/docker-compose.es.yml up -d elasticsearch
     */
    static class RealEsAggregation {

        static void run() throws Exception {
            // 创建 ES 低级 REST 客户端
            org.apache.http.HttpHost host = new org.apache.http.HttpHost("localhost", 9200, "http");
            org.elasticsearch.client.RestClient restClient =
                    org.elasticsearch.client.RestClient.builder(host).build();

            // 创建 ES Java Client（基于 JSON 的类型安全 API）
            co.elastic.clients.transport.ElasticsearchTransport transport =
                    new co.elastic.clients.transport.rest_client.RestClientTransport(
                            restClient,
                            new co.elastic.clients.json.jackson.JacksonJsonpMapper());
            co.elastic.clients.elasticsearch.ElasticsearchClient client =
                    new co.elastic.clients.elasticsearch.ElasticsearchClient(transport);

            String indexName = "products_demo";

            try {
                // 1. 创建索引并写入测试数据
                prepareTestData(client, indexName);

                // 2. Terms 聚合：按类目分组
                demoRealTermsAgg(client, indexName);

                // 3. Stats 聚合：价格统计
                demoRealStatsAgg(client, indexName);

                // 4. Range 聚合：按价格区间分桶
                demoRealRangeAgg(client, indexName);

                // 5. 嵌套聚合：每个品牌的平均价格
                demoRealNestedAgg(client, indexName);

            } finally {
                // 清理测试数据（可注释掉以保留数据，方便在 Kibana 中查看）
                // client.indices().delete(d -> d.index(indexName));
                // System.out.println("  清理：已删除测试索引 " + indexName);
                cleanup(client, indexName);
                restClient.close();
            }
        }

        /**
         * 清理测试索引。如需保留数据在 Kibana 中查看，注释掉 delete 行即可
         */
        static void cleanup(co.elastic.clients.elasticsearch.ElasticsearchClient client,
                            String indexName) throws Exception {
            client.indices().delete(d -> d.index(indexName));
            System.out.println("  清理：已删除测试索引 " + indexName);
            System.out.println("  提示：如需保留数据，注释掉 cleanup() 调用即可");
        }
    }

    /**
     * 写入测试数据
     */
    static void prepareTestData(co.elastic.clients.elasticsearch.ElasticsearchClient client,
                                String indexName) throws Exception {
        // 删除旧索引（如果存在）
        if (client.indices().exists(e -> e.index(indexName)).value()) {
            client.indices().delete(d -> d.index(indexName));
        }

        // 批量写入
        co.elastic.clients.elasticsearch.core.BulkRequest.Builder bulkBuilder =
                new co.elastic.clients.elasticsearch.core.BulkRequest.Builder();

        String[][] products = {
                {"iPhone 15", "手机", "Apple", "5999", "1200"},
                {"iPhone 15 Pro", "手机", "Apple", "8999", "800"},
                {"Mate 60", "手机", "华为", "4999", "1500"},
                {"小米 14", "手机", "小米", "3999", "2000"},
                {"MacBook Pro", "电脑", "Apple", "14999", "500"},
                {"ThinkPad X1", "电脑", "联想", "9999", "600"},
                {"AirPods Pro", "耳机", "Apple", "1799", "3000"},
                {"FreeBuds Pro", "耳机", "华为", "999", "2500"},
        };

        for (int i = 0; i < products.length; i++) {
            String[] p = products[i];
            java.util.Map<String, Object> doc = new java.util.LinkedHashMap<>();
            doc.put("name", p[0]);
            doc.put("category", p[1]);
            doc.put("brand", p[2]);
            doc.put("price", Double.parseDouble(p[3]));
            doc.put("sales", Integer.parseInt(p[4]));

            final int id = i + 1;
            bulkBuilder.operations(op -> op.index(idx -> idx
                    .index(indexName).id(String.valueOf(id)).document(doc)));
        }

        client.bulk(bulkBuilder.build());
        // 刷新索引使数据可搜索
        client.indices().refresh(r -> r.index(indexName));
        System.out.println("  准备数据：写入 " + products.length + " 条商品到 " + indexName);
        System.out.println();
    }

    /**
     * Terms 聚合：按类目分组统计
     */
    static void demoRealTermsAgg(co.elastic.clients.elasticsearch.ElasticsearchClient client,
                                 String indexName) throws Exception {
        System.out.println("  【真实 ES — Terms 聚合】按类目分组");

        var response = client.search(s -> s
                        .index(indexName)
                        .size(0) // 不返回文档，只要聚合结果
                        .aggregations("by_category", a -> a
                                .terms(t -> t.field("category.keyword"))),
                java.util.Map.class);

        var buckets = response.aggregations().get("by_category").sterms().buckets().array();
        for (var bucket : buckets) {
            System.out.printf("    %s: %d 件%n", bucket.key().stringValue(), bucket.docCount());
        }
        System.out.println();
    }

    /**
     * Stats 聚合：价格统计
     */
    static void demoRealStatsAgg(co.elastic.clients.elasticsearch.ElasticsearchClient client,
                                 String indexName) throws Exception {
        System.out.println("  【真实 ES — Stats 聚合】价格统计");

        var response = client.search(s -> s
                        .index(indexName)
                        .size(0)
                        .aggregations("price_stats", a -> a
                                .stats(st -> st.field("price"))),
                java.util.Map.class);

        var stats = response.aggregations().get("price_stats").stats();
        System.out.printf("    count=%d, min=%.0f, max=%.0f, avg=%.2f, sum=%.0f%n",
                stats.count(), stats.min(), stats.max(), stats.avg(), stats.sum());
        System.out.println();
    }

    /**
     * Range 聚合：按价格区间分桶
     */
    static void demoRealRangeAgg(co.elastic.clients.elasticsearch.ElasticsearchClient client,
                                 String indexName) throws Exception {
        System.out.println("  【真实 ES — Range 聚合】按价格区间分桶");

        var response = client.search(s -> s
                        .index(indexName)
                        .size(0)
                        .aggregations("price_ranges", a -> a
                                .range(r -> r
                                        .field("price")
                                        .ranges(rng -> rng.to("2000"))
                                        .ranges(rng -> rng.from("2000").to("6000"))
                                        .ranges(rng -> rng.from("6000").to("10000"))
                                        .ranges(rng -> rng.from("10000")))),
                java.util.Map.class);

        var buckets = response.aggregations().get("price_ranges").range().buckets().array();
        for (var bucket : buckets) {
            System.out.printf("    %s: %d 件%n", bucket.key(), bucket.docCount());
        }
        System.out.println();
    }

    /**
     * 嵌套聚合：每个品牌的平均价格
     */
    static void demoRealNestedAgg(co.elastic.clients.elasticsearch.ElasticsearchClient client,
                                  String indexName) throws Exception {
        System.out.println("  【真实 ES — 嵌套聚合】每个品牌的平均价格");

        var response = client.search(s -> s
                        .index(indexName)
                        .size(0)
                        .aggregations("by_brand", a -> a
                                .terms(t -> t.field("brand.keyword"))
                                .aggregations("avg_price", sub -> sub
                                        .avg(avg -> avg.field("price")))),
                java.util.Map.class);

        var buckets = response.aggregations().get("by_brand").sterms().buckets().array();
        for (var bucket : buckets) {
            double avgPrice = bucket.aggregations().get("avg_price").avg().value();
            System.out.printf("    %s: %d 件, 平均价格 %.0f 元%n",
                    bucket.key().stringValue(), bucket.docCount(), avgPrice);
        }
        System.out.println();
    }

}
