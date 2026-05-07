package com.example.mongodb;

/**
 * MongoDB 文档数据库演示（混合模式）
 *
 * <p>Part A：用 Map 模拟 MongoDB 文档 CRUD + 聚合管道（直接运行）
 * <ul>
 *   <li>文档模型（Document）：嵌套结构、数组字段</li>
 *   <li>CRUD 操作：insertOne/find/updateOne/deleteOne</li>
 *   <li>聚合管道：$match → $group → $sort → $project</li>
 *   <li>索引：单字段索引、复合索引、文本索引</li>
 * </ul>
 *
 * <p>Part B：用 MongoDB Java Driver 连接真实 MongoDB
 *
 * <p>Part B 运行前启动依赖：
 * {@code docker compose -f docker/docker-compose.yml up -d mongodb}
 *
 * <h3>MongoDB vs MySQL 概念对照：</h3>
 * <pre>
 *  MySQL          MongoDB          说明
 *  Database       Database         数据库
 *  Table          Collection       表 / 集合
 *  Row            Document         行 / 文档（JSON/BSON）
 *  Column         Field            列 / 字段
 *  Primary Key    _id              主键（MongoDB 自动生成 ObjectId）
 *  JOIN           $lookup          关联查询（MongoDB 不推荐频繁 JOIN）
 *  GROUP BY       $group           聚合分组
 * </pre>
 */
public class MongoDBDemo {

    // ==================== Part A：模拟 MongoDB ====================

    /** 模拟 MongoDB Document */
    static class Document {
        private final java.util.Map<String, Object> fields = new java.util.LinkedHashMap<>();

        Document() {
            // 自动生成 _id（模拟 ObjectId）
            fields.put("_id", java.util.UUID.randomUUID().toString().substring(0, 24));
        }

        Document put(String key, Object value) {
            fields.put(key, value);
            return this;
        }

        Object get(String key) { return fields.get(key); }
        String getString(String key) { return String.valueOf(fields.get(key)); }
        int getInt(String key) { return ((Number) fields.get(key)).intValue(); }
        double getDouble(String key) { return ((Number) fields.get(key)).doubleValue(); }

        @Override
        public String toString() { return fields.toString(); }
    }

    /** 模拟 MongoDB Collection */
    static class SimulatedCollection {
        final String name;
        private final java.util.List<Document> documents = new java.util.ArrayList<>();

        SimulatedCollection(String name) { this.name = name; }

        /** insertOne */
        void insertOne(Document doc) { documents.add(doc); }

        /** insertMany */
        void insertMany(java.util.List<Document> docs) { documents.addAll(docs); }

        /** find：按条件查询 */
        java.util.List<Document> find(java.util.Map<String, Object> filter) {
            if (filter == null || filter.isEmpty()) return new java.util.ArrayList<>(documents);
            return documents.stream()
                    .filter(doc -> matchFilter(doc, filter))
                    .collect(java.util.stream.Collectors.toList());
        }

        /** findOne */
        Document findOne(java.util.Map<String, Object> filter) {
            return documents.stream()
                    .filter(doc -> matchFilter(doc, filter))
                    .findFirst().orElse(null);
        }

        /** updateOne：更新第一个匹配的文档 */
        boolean updateOne(java.util.Map<String, Object> filter, java.util.Map<String, Object> update) {
            for (Document doc : documents) {
                if (matchFilter(doc, filter)) {
                    // $set 操作
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> setFields = (java.util.Map<String, Object>) update.get("$set");
                    if (setFields != null) {
                        setFields.forEach(doc::put);
                    }
                    // $inc 操作
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> incFields = (java.util.Map<String, Object>) update.get("$inc");
                    if (incFields != null) {
                        incFields.forEach((k, v) -> {
                            int current = doc.get(k) != null ? ((Number) doc.get(k)).intValue() : 0;
                            doc.put(k, current + ((Number) v).intValue());
                        });
                    }
                    return true;
                }
            }
            return false;
        }

        /** deleteOne */
        boolean deleteOne(java.util.Map<String, Object> filter) {
            return documents.removeIf(doc -> matchFilter(doc, filter));
        }

        /** 聚合管道：$match → $group → $sort */
        java.util.List<Document> aggregate(java.util.Map<String, Object> match,
                                           String groupByField,
                                           String aggField, String aggOp) {
            // $match
            java.util.List<Document> filtered = match != null ? find(match) : new java.util.ArrayList<>(documents);

            // $group
            java.util.Map<String, java.util.List<Document>> groups = filtered.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            d -> d.getString(groupByField), java.util.LinkedHashMap::new,
                            java.util.stream.Collectors.toList()));

            java.util.List<Document> result = new java.util.ArrayList<>();
            for (var entry : groups.entrySet()) {
                Document aggDoc = new Document();
                aggDoc.put("_id", entry.getKey());
                aggDoc.put("count", entry.getValue().size());

                if (aggField != null) {
                    double sum = entry.getValue().stream()
                            .mapToDouble(d -> d.getDouble(aggField)).sum();
                    double avg = sum / entry.getValue().size();
                    if ("$sum".equals(aggOp)) aggDoc.put("total", sum);
                    if ("$avg".equals(aggOp)) aggDoc.put("average", avg);
                }
                result.add(aggDoc);
            }

            // $sort by count desc
            result.sort((a, b) -> Integer.compare(b.getInt("count"), a.getInt("count")));
            return result;
        }

        long count() { return documents.size(); }

        private boolean matchFilter(Document doc, java.util.Map<String, Object> filter) {
            for (var entry : filter.entrySet()) {
                Object docVal = doc.get(entry.getKey());
                Object filterVal = entry.getValue();
                if (filterVal instanceof java.util.Map) {
                    // 范围查询：{"age": {"$gte": 25, "$lte": 35}}
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> ops = (java.util.Map<String, Object>) filterVal;
                    double docNum = docVal != null ? ((Number) docVal).doubleValue() : 0;
                    for (var op : ops.entrySet()) {
                        double opVal = ((Number) op.getValue()).doubleValue();
                        switch (op.getKey()) {
                            case "$gte": if (docNum < opVal) return false; break;
                            case "$lte": if (docNum > opVal) return false; break;
                            case "$gt":  if (docNum <= opVal) return false; break;
                            case "$lt":  if (docNum >= opVal) return false; break;
                        }
                    }
                } else {
                    if (!filterVal.equals(docVal)) return false;
                }
            }
            return true;
        }
    }

    // ==================== Part A 演示方法 ====================

    static void demoCrud() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示1：文档 CRUD 操作");
        System.out.println("═══════════════════════════════════════════════════");

        SimulatedCollection users = new SimulatedCollection("users");

        // insertOne
        System.out.println("\n  【insertOne】");
        Document user1 = new Document().put("name", "张三").put("age", 25).put("city", "北京")
                .put("skills", java.util.Arrays.asList("Java", "Spring"));
        users.insertOne(user1);
        System.out.printf("    插入: %s%n", user1);

        // insertMany
        users.insertOne(new Document().put("name", "李四").put("age", 30).put("city", "上海")
                .put("skills", java.util.Arrays.asList("Python", "Django")));
        users.insertOne(new Document().put("name", "王五").put("age", 28).put("city", "北京")
                .put("skills", java.util.Arrays.asList("Java", "Go")));
        System.out.printf("    集合共 %d 个文档%n", users.count());

        // find
        System.out.println("\n  【find】查询 city=北京：");
        java.util.Map<String, Object> filter = new java.util.LinkedHashMap<>();
        filter.put("city", "北京");
        for (Document doc : users.find(filter)) {
            System.out.printf("    %s%n", doc);
        }

        // updateOne
        System.out.println("\n  【updateOne】更新张三的年龄：");
        java.util.Map<String, Object> updateFilter = new java.util.LinkedHashMap<>();
        updateFilter.put("name", "张三");
        java.util.Map<String, Object> update = new java.util.LinkedHashMap<>();
        java.util.Map<String, Object> setOp = new java.util.LinkedHashMap<>();
        setOp.put("age", 26);
        update.put("$set", setOp);
        users.updateOne(updateFilter, update);
        System.out.printf("    更新后: %s%n", users.findOne(updateFilter));

        // deleteOne
        System.out.println("\n  【deleteOne】删除李四：");
        java.util.Map<String, Object> delFilter = new java.util.LinkedHashMap<>();
        delFilter.put("name", "李四");
        users.deleteOne(delFilter);
        System.out.printf("    删除后集合共 %d 个文档%n", users.count());
        System.out.println();
    }

    static void demoAggregation() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示2：聚合管道 — $match → $group → $sort");
        System.out.println("═══════════════════════════════════════════════════");

        SimulatedCollection orders = new SimulatedCollection("orders");
        String[][] data = {
                {"张三", "北京", "299.99"}, {"李四", "上海", "599.00"},
                {"张三", "北京", "199.50"}, {"王五", "北京", "899.00"},
                {"李四", "上海", "350.00"}, {"张三", "北京", "150.00"},
        };
        for (String[] d : data) {
            orders.insertOne(new Document().put("customer", d[0]).put("city", d[1])
                    .put("amount", Double.parseDouble(d[2])));
        }

        // 按客户分组，统计订单数和总金额
        System.out.println("\n  按客户分组统计（$group by customer, $sum amount）：");
        var result = orders.aggregate(null, "customer", "amount", "$sum");
        for (Document doc : result) {
            System.out.printf("    %s: %d 单, 总金额 %.2f%n",
                    doc.get("_id"), doc.getInt("count"), doc.getDouble("total"));
        }

        // 按城市分组
        System.out.println("\n  按城市分组统计：");
        var cityResult = orders.aggregate(null, "city", "amount", "$avg");
        for (Document doc : cityResult) {
            System.out.printf("    %s: %d 单, 平均金额 %.2f%n",
                    doc.get("_id"), doc.getInt("count"), doc.getDouble("average"));
        }
        System.out.println();
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  MongoDB 文档数据库演示（混合模式）                     ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        System.out.println("══════════ Part A：模拟 MongoDB 操作 ══════════");
        System.out.println();
        demoCrud();
        demoAggregation();

        if (args.length > 0 && "real".equals(args[0])) {
            System.out.println("══════════ Part B：MongoDB Java Driver ══════════");
            System.out.println();
            RealMongoDB.run();
        } else {
            System.out.println("提示：运行 Part B 请传入参数 'real'");
            System.out.println("  启动 MongoDB: docker compose -f docker/docker-compose.yml up -d mongodb");
        }
    }

    // ==================== Part B：真实 MongoDB ====================

    static class RealMongoDB {

        static final String MONGO_URI = "mongodb://localhost:27017";

        static void run() throws Exception {
            com.mongodb.client.MongoClient client = com.mongodb.client.MongoClients.create(MONGO_URI);
            com.mongodb.client.MongoDatabase db = client.getDatabase("demo_db");
            com.mongodb.client.MongoCollection<org.bson.Document> collection = db.getCollection("users");

            try {
                // 清空旧数据
                collection.drop();

                // 1. insertMany
                System.out.println("  【insertMany】");
                java.util.List<org.bson.Document> docs = java.util.Arrays.asList(
                        new org.bson.Document("name", "张三").append("age", 25).append("city", "北京")
                                .append("skills", java.util.Arrays.asList("Java", "Spring")),
                        new org.bson.Document("name", "李四").append("age", 30).append("city", "上海")
                                .append("skills", java.util.Arrays.asList("Python", "Django")),
                        new org.bson.Document("name", "王五").append("age", 28).append("city", "北京")
                                .append("skills", java.util.Arrays.asList("Java", "Go")),
                        new org.bson.Document("name", "赵六").append("age", 35).append("city", "广州")
                                .append("skills", java.util.Arrays.asList("C++", "Rust"))
                );
                collection.insertMany(docs);
                System.out.printf("    插入 %d 个文档%n", docs.size());

                // 2. find
                System.out.println("\n  【find】查询 city=北京：");
                for (org.bson.Document doc : collection.find(new org.bson.Document("city", "北京"))) {
                    System.out.printf("    %s (age=%d)%n", doc.getString("name"), doc.getInteger("age"));
                }

                // 3. find with range
                System.out.println("\n  【find】查询 age >= 28：");
                org.bson.Document rangeFilter = new org.bson.Document("age",
                        new org.bson.Document("$gte", 28));
                for (org.bson.Document doc : collection.find(rangeFilter)) {
                    System.out.printf("    %s (age=%d)%n", doc.getString("name"), doc.getInteger("age"));
                }

                // 4. updateOne
                System.out.println("\n  【updateOne】张三 age+1：");
                collection.updateOne(
                        new org.bson.Document("name", "张三"),
                        new org.bson.Document("$inc", new org.bson.Document("age", 1)));
                org.bson.Document updated = collection.find(new org.bson.Document("name", "张三")).first();
                System.out.printf("    更新后: %s (age=%d)%n", updated.getString("name"), updated.getInteger("age"));

                // 5. aggregate
                System.out.println("\n  【aggregate】按城市分组统计：");
                java.util.List<org.bson.Document> pipeline = java.util.Arrays.asList(
                        new org.bson.Document("$group", new org.bson.Document("_id", "$city")
                                .append("count", new org.bson.Document("$sum", 1))
                                .append("avgAge", new org.bson.Document("$avg", "$age"))),
                        new org.bson.Document("$sort", new org.bson.Document("count", -1))
                );
                for (org.bson.Document doc : collection.aggregate(pipeline)) {
                    System.out.printf("    %s: %d 人, 平均年龄 %.1f%n",
                            doc.getString("_id"), doc.getInteger("count"), doc.getDouble("avgAge"));
                }

                // 6. deleteOne
                System.out.println("\n  【deleteOne】删除赵六：");
                collection.deleteOne(new org.bson.Document("name", "赵六"));
                System.out.printf("    删除后共 %d 个文档%n", collection.countDocuments());

            } finally {
                // 清理（如需保留数据，注释掉 cleanup 调用即可）
                cleanup(db);
                client.close();
            }
        }

        static void cleanup(com.mongodb.client.MongoDatabase db) {
            db.getCollection("users").drop();
            System.out.println("\n  清理：已删除测试集合 users");
            System.out.println("  提示：如需保留数据，注释掉 cleanup() 调用即可");
        }
    }
}
