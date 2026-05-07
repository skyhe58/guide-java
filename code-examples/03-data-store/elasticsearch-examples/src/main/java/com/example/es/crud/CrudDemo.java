package com.example.es.crud;

/**
 * Elasticsearch 文档 CRUD 演示（混合模式）
 *
 * <p>Part A：用 ConcurrentHashMap 模拟 ES 文档存储（直接运行）
 * <ul>
 *   <li>文档的增删改查（Index / Get / Update / Delete）</li>
 *   <li>版本控制与乐观锁（_version + _seq_no + _primary_term）</li>
 *   <li>批量操作（Bulk API）</li>
 *   <li>文档部分更新 vs 全量替换</li>
 * </ul>
 *
 * <p>Part B：用 ES Java Client 连接真实 Elasticsearch 执行 CRUD
 *
 * <p>Part B 运行前启动依赖：
 * {@code docker compose -f docker/docker-compose.es.yml up -d elasticsearch}
 */
public class CrudDemo {

    // ==================== Part A：内存模拟 ====================

    /** 文档版本信息，模拟 ES 的 _version / _seq_no / _primary_term */
    static class VersionInfo {
        long version;       // 文档版本号，每次更新 +1
        long seqNo;         // 序列号，全局递增
        long primaryTerm;   // 主分片任期号

        VersionInfo() {
            this.version = 1;
            this.seqNo = 0;
            this.primaryTerm = 1;
        }

        void increment(long globalSeqNo) {
            this.version++;
            this.seqNo = globalSeqNo;
        }
    }

    /** 模拟 ES 索引（一个索引 = 一张表） */
    static class SimulatedIndex {
        private final String name;
        // 文档存储：_id → 文档内容（JSON 用 Map 模拟）
        private final java.util.concurrent.ConcurrentHashMap<String, java.util.Map<String, Object>> docs =
                new java.util.concurrent.ConcurrentHashMap<>();
        // 版本信息：_id → VersionInfo
        private final java.util.concurrent.ConcurrentHashMap<String, VersionInfo> versions =
                new java.util.concurrent.ConcurrentHashMap<>();
        // 全局序列号
        private final java.util.concurrent.atomic.AtomicLong globalSeqNo =
                new java.util.concurrent.atomic.AtomicLong(0);

        SimulatedIndex(String name) {
            this.name = name;
        }

        /**
         * Index 操作：创建或全量替换文档。
         * 对应 PUT /{index}/_doc/{id}
         * 如果文档已存在，全量替换（版本号 +1）
         */
        IndexResult index(String id, java.util.Map<String, Object> doc) {
            boolean created = !docs.containsKey(id);
            docs.put(id, new java.util.LinkedHashMap<>(doc));

            VersionInfo vi = versions.computeIfAbsent(id, k -> new VersionInfo());
            if (!created) {
                vi.increment(globalSeqNo.incrementAndGet());
            } else {
                vi.seqNo = globalSeqNo.incrementAndGet();
            }
            return new IndexResult(id, vi.version, vi.seqNo, created ? "created" : "updated");
        }

        /**
         * Get 操作：根据 _id 获取文档。
         * 对应 GET /{index}/_doc/{id}
         * 实时读取，不经过倒排索引（区别于 Search）
         */
        GetResult get(String id) {
            java.util.Map<String, Object> doc = docs.get(id);
            if (doc == null) return new GetResult(id, false, null, 0, 0);
            VersionInfo vi = versions.get(id);
            return new GetResult(id, true, new java.util.LinkedHashMap<>(doc), vi.version, vi.seqNo);
        }

        /**
         * Update 操作：部分更新文档（合并字段）。
         * 对应 POST /{index}/_update/{id}
         * 只更新指定字段，其他字段保留（区别于 Index 的全量替换）
         */
        IndexResult update(String id, java.util.Map<String, Object> partialDoc) {
            java.util.Map<String, Object> existing = docs.get(id);
            if (existing == null) {
                throw new IllegalArgumentException("文档不存在: " + id);
            }
            // 合并字段（部分更新的核心：只覆盖传入的字段）
            existing.putAll(partialDoc);

            VersionInfo vi = versions.get(id);
            vi.increment(globalSeqNo.incrementAndGet());
            return new IndexResult(id, vi.version, vi.seqNo, "updated");
        }

        /**
         * 乐观锁更新：基于 _seq_no 和 _primary_term 的并发控制。
         * 对应 PUT /{index}/_doc/{id}?if_seq_no=X&if_primary_term=Y
         * 如果版本不匹配，抛出 VersionConflictException
         */
        IndexResult updateWithOptimisticLock(String id, java.util.Map<String, Object> doc,
                                             long expectedSeqNo, long expectedPrimaryTerm) {
            VersionInfo vi = versions.get(id);
            if (vi == null) throw new IllegalArgumentException("文档不存在: " + id);

            // 乐观锁检查：seq_no 和 primary_term 必须匹配
            if (vi.seqNo != expectedSeqNo || vi.primaryTerm != expectedPrimaryTerm) {
                throw new RuntimeException(String.format(
                        "VersionConflictException: 期望 seq_no=%d, primary_term=%d，" +
                                "实际 seq_no=%d, primary_term=%d",
                        expectedSeqNo, expectedPrimaryTerm, vi.seqNo, vi.primaryTerm));
            }

            docs.put(id, new java.util.LinkedHashMap<>(doc));
            vi.increment(globalSeqNo.incrementAndGet());
            return new IndexResult(id, vi.version, vi.seqNo, "updated");
        }

        /**
         * Delete 操作：删除文档。
         * 对应 DELETE /{index}/_doc/{id}
         * ES 中删除不会立即物理删除，而是标记为 deleted，后续 merge 时清理
         */
        boolean delete(String id) {
            boolean existed = docs.remove(id) != null;
            versions.remove(id);
            return existed;
        }

        /** Bulk 批量操作 */
        java.util.List<String> bulk(java.util.List<BulkItem> items) {
            java.util.List<String> results = new java.util.ArrayList<>();
            for (BulkItem item : items) {
                try {
                    switch (item.action) {
                        case "index":
                            IndexResult ir = index(item.id, item.doc);
                            results.add(String.format("  %s %s → %s (v%d)", item.action, item.id, ir.result, ir.version));
                            break;
                        case "delete":
                            boolean deleted = delete(item.id);
                            results.add(String.format("  %s %s → %s", item.action, item.id, deleted ? "deleted" : "not_found"));
                            break;
                        case "update":
                            IndexResult ur = update(item.id, item.doc);
                            results.add(String.format("  %s %s → %s (v%d)", item.action, item.id, ur.result, ur.version));
                            break;
                    }
                } catch (Exception e) {
                    results.add(String.format("  %s %s → ERROR: %s", item.action, item.id, e.getMessage()));
                }
            }
            return results;
        }

        int size() { return docs.size(); }
    }

    // ==================== 结果模型 ====================

    static class IndexResult {
        final String id;
        final long version;
        final long seqNo;
        final String result; // created / updated

        IndexResult(String id, long version, long seqNo, String result) {
            this.id = id; this.version = version; this.seqNo = seqNo; this.result = result;
        }
    }

    static class GetResult {
        final String id;
        final boolean found;
        final java.util.Map<String, Object> source;
        final long version;
        final long seqNo;

        GetResult(String id, boolean found, java.util.Map<String, Object> source, long version, long seqNo) {
            this.id = id; this.found = found; this.source = source; this.version = version; this.seqNo = seqNo;
        }
    }

    static class BulkItem {
        final String action;
        final String id;
        final java.util.Map<String, Object> doc;

        BulkItem(String action, String id, java.util.Map<String, Object> doc) {
            this.action = action; this.id = id; this.doc = doc;
        }
    }

    // ==================== Part A 演示方法 ====================

    /** 演示1：基本 CRUD */
    static void demoCrud() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示1：文档 CRUD — Index / Get / Update / Delete");
        System.out.println("═══════════════════════════════════════════════════");

        SimulatedIndex index = new SimulatedIndex("users");

        // Create（Index）
        java.util.Map<String, Object> user1 = new java.util.LinkedHashMap<>();
        user1.put("name", "张三");
        user1.put("age", 25);
        user1.put("city", "北京");
        IndexResult r1 = index.index("1", user1);
        System.out.printf("\n  PUT /users/_doc/1 → %s (version=%d, seq_no=%d)%n", r1.result, r1.version, r1.seqNo);

        // Read（Get）
        GetResult g1 = index.get("1");
        System.out.printf("  GET /users/_doc/1 → found=%s, _source=%s%n", g1.found, g1.source);

        // Update（部分更新）
        java.util.Map<String, Object> partial = new java.util.LinkedHashMap<>();
        partial.put("age", 26);
        partial.put("email", "zhangsan@example.com");
        IndexResult r2 = index.update("1", partial);
        System.out.printf("  POST /users/_update/1 → %s (version=%d)%n", r2.result, r2.version);

        GetResult g2 = index.get("1");
        System.out.printf("  GET /users/_doc/1 → %s（age 更新，name/city 保留）%n", g2.source);

        // Delete
        boolean deleted = index.delete("1");
        System.out.printf("  DELETE /users/_doc/1 → %s%n", deleted ? "deleted" : "not_found");

        GetResult g3 = index.get("1");
        System.out.printf("  GET /users/_doc/1 → found=%s%n", g3.found);
        System.out.println();
    }

    /** 演示2：全量替换 vs 部分更新 */
    static void demoIndexVsUpdate() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示2：Index（全量替换）vs Update（部分更新）");
        System.out.println("═══════════════════════════════════════════════════");

        SimulatedIndex index = new SimulatedIndex("users");

        java.util.Map<String, Object> doc = new java.util.LinkedHashMap<>();
        doc.put("name", "李四");
        doc.put("age", 30);
        doc.put("city", "上海");
        doc.put("phone", "13800138000");
        index.index("2", doc);
        System.out.printf("\n  初始文档: %s%n", index.get("2").source);

        // Index 全量替换：只传 name 和 age，其他字段会丢失！
        java.util.Map<String, Object> replaceDoc = new java.util.LinkedHashMap<>();
        replaceDoc.put("name", "李四");
        replaceDoc.put("age", 31);
        index.index("2", replaceDoc);
        System.out.printf("  Index 全量替换后: %s（city 和 phone 丢失！）%n", index.get("2").source);

        // 重新初始化
        index.index("2", doc);

        // Update 部分更新：只更新 age，其他字段保留
        java.util.Map<String, Object> partialDoc = new java.util.LinkedHashMap<>();
        partialDoc.put("age", 31);
        index.update("2", partialDoc);
        System.out.printf("  Update 部分更新后: %s（只改 age，其他保留）%n", index.get("2").source);

        System.out.println("\n  结论：");
        System.out.println("    PUT /_doc/{id}  → 全量替换，未传的字段会丢失");
        System.out.println("    POST /_update/{id} → 部分更新，只覆盖传入的字段");
        System.out.println();
    }

    /** 演示3：乐观锁并发控制 */
    static void demoOptimisticLock() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示3：乐观锁 — if_seq_no + if_primary_term");
        System.out.println("═══════════════════════════════════════════════════");

        SimulatedIndex index = new SimulatedIndex("products");

        java.util.Map<String, Object> product = new java.util.LinkedHashMap<>();
        product.put("name", "iPhone 15");
        product.put("stock", 100);
        index.index("p1", product);

        GetResult g = index.get("p1");
        System.out.printf("\n  初始: stock=%s, seq_no=%d%n", g.source.get("stock"), g.seqNo);

        // 用户 A 读取后准备更新
        long userASeqNo = g.seqNo;

        // 用户 B 先更新成功
        java.util.Map<String, Object> updateB = new java.util.LinkedHashMap<>(g.source);
        updateB.put("stock", 99);
        IndexResult rb = index.updateWithOptimisticLock("p1", updateB, userASeqNo, 1);
        System.out.printf("  用户B 更新: stock=99 → %s (new seq_no=%d)%n", rb.result, rb.seqNo);

        // 用户 A 用旧的 seq_no 更新 → 冲突！
        java.util.Map<String, Object> updateA = new java.util.LinkedHashMap<>(g.source);
        updateA.put("stock", 98);
        try {
            index.updateWithOptimisticLock("p1", updateA, userASeqNo, 1);
        } catch (RuntimeException e) {
            System.out.printf("  用户A 更新: stock=98 → 失败！%s%n", e.getMessage());
        }

        System.out.printf("  最终: %s（用户B的更新生效，用户A需要重试）%n", index.get("p1").source);
        System.out.println();
    }

    /** 演示4：Bulk 批量操作 */
    static void demoBulk() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示4：Bulk 批量操作");
        System.out.println("═══════════════════════════════════════════════════");

        SimulatedIndex index = new SimulatedIndex("users");

        java.util.List<BulkItem> items = new java.util.ArrayList<>();
        java.util.Map<String, Object> u1 = new java.util.LinkedHashMap<>();
        u1.put("name", "张三"); u1.put("age", 25);
        items.add(new BulkItem("index", "1", u1));

        java.util.Map<String, Object> u2 = new java.util.LinkedHashMap<>();
        u2.put("name", "李四"); u2.put("age", 30);
        items.add(new BulkItem("index", "2", u2));

        java.util.Map<String, Object> u3 = new java.util.LinkedHashMap<>();
        u3.put("name", "王五"); u3.put("age", 28);
        items.add(new BulkItem("index", "3", u3));

        items.add(new BulkItem("delete", "2", null));

        java.util.Map<String, Object> u1Update = new java.util.LinkedHashMap<>();
        u1Update.put("age", 26);
        items.add(new BulkItem("update", "1", u1Update));

        System.out.println("\n  POST /_bulk（5 个操作）：");
        java.util.List<String> results = index.bulk(items);
        for (String r : results) {
            System.out.println("  " + r);
        }
        System.out.printf("\n  索引中剩余 %d 个文档%n", index.size());
        System.out.println();
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  Elasticsearch 文档 CRUD 演示（混合模式）              ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        // ===== Part A：纯内存模拟 =====
        System.out.println("══════════ Part A：内存模拟文档 CRUD ══════════");
        System.out.println();
        demoCrud();
        demoIndexVsUpdate();
        demoOptimisticLock();
        demoBulk();

        // ===== Part B：连接真实 ES =====
        if (args.length > 0 && "real".equals(args[0])) {
            System.out.println("══════════ Part B：连接真实 Elasticsearch ══════════");
            System.out.println();
            RealEsCrud.run();
        } else {
            System.out.println("提示：运行 Part B（真实 ES CRUD）请传入参数 'real'");
        }
    }

    // ==================== Part B：真实 ES CRUD ====================

    static class RealEsCrud {

        static void run() throws Exception {
            org.apache.http.HttpHost host = new org.apache.http.HttpHost("localhost", 9200, "http");
            org.elasticsearch.client.RestClient restClient =
                    org.elasticsearch.client.RestClient.builder(host).build();
            co.elastic.clients.transport.ElasticsearchTransport transport =
                    new co.elastic.clients.transport.rest_client.RestClientTransport(
                            restClient, new co.elastic.clients.json.jackson.JacksonJsonpMapper());
            co.elastic.clients.elasticsearch.ElasticsearchClient client =
                    new co.elastic.clients.elasticsearch.ElasticsearchClient(transport);

            String indexName = "crud_demo";

            try {
                // 清理旧索引
                if (client.indices().exists(e -> e.index(indexName)).value()) {
                    client.indices().delete(d -> d.index(indexName));
                }

                // 1. Index 创建文档
                System.out.println("  【Index 创建文档】");
                java.util.Map<String, Object> user = new java.util.LinkedHashMap<>();
                user.put("name", "张三");
                user.put("age", 25);
                user.put("city", "北京");

                var indexResp = client.index(i -> i.index(indexName).id("1").document(user));
                System.out.printf("    PUT /%s/_doc/1 → result=%s, version=%d, seq_no=%d%n",
                        indexName, indexResp.result(), indexResp.version(), indexResp.seqNo());

                // 2. Get 获取文档
                System.out.println("\n  【Get 获取文档】");
                var getResp = client.get(g -> g.index(indexName).id("1"), java.util.Map.class);
                System.out.printf("    GET /%s/_doc/1 → found=%s, _source=%s%n",
                        indexName, getResp.found(), getResp.source());

                // 3. Update 部分更新
                System.out.println("\n  【Update 部分更新】");
                java.util.Map<String, Object> partial = new java.util.LinkedHashMap<>();
                partial.put("age", 26);
                partial.put("email", "zhangsan@example.com");

                var updateResp = client.update(u -> u
                                .index(indexName).id("1").doc(partial).docAsUpsert(false),
                        java.util.Map.class);
                System.out.printf("    POST /%s/_update/1 → result=%s, version=%d%n",
                        indexName, updateResp.result(), updateResp.version());

                var getResp2 = client.get(g -> g.index(indexName).id("1"), java.util.Map.class);
                System.out.printf("    更新后: %s%n", getResp2.source());

                // 4. Bulk 批量操作
                System.out.println("\n  【Bulk 批量操作】");
                java.util.Map<String, Object> u2 = new java.util.LinkedHashMap<>();
                u2.put("name", "李四"); u2.put("age", 30); u2.put("city", "上海");
                java.util.Map<String, Object> u3 = new java.util.LinkedHashMap<>();
                u3.put("name", "王五"); u3.put("age", 28); u3.put("city", "广州");

                var bulkResp = client.bulk(b -> b
                        .operations(op -> op.index(idx -> idx.index(indexName).id("2").document(u2)))
                        .operations(op -> op.index(idx -> idx.index(indexName).id("3").document(u3)))
                        .operations(op -> op.delete(del -> del.index(indexName).id("1"))));

                System.out.printf("    Bulk 执行 %d 个操作，errors=%s%n",
                        bulkResp.items().size(), bulkResp.errors());
                for (var item : bulkResp.items()) {
                    System.out.printf("    %s %s → %s%n",
                            item.operationType(), item.id(), item.result());
                }

                // 5. Delete 删除文档
                System.out.println("\n  【Delete 删除文档】");
                var delResp = client.delete(d -> d.index(indexName).id("2"));
                System.out.printf("    DELETE /%s/_doc/2 → result=%s%n", indexName, delResp.result());

            } finally {
                // 清理测试索引（如需保留数据在 Kibana 中查看，注释掉 cleanup 调用即可）
                cleanup(client, indexName);
                restClient.close();
            }
        }

        /** 清理测试索引。如需保留数据，注释掉 cleanup() 调用即可 */
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
