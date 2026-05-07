package com.example.redis.datastructure;

/**
 * Redis 五种数据结构演示（混合模式）
 *
 * <p>Part A：用 Java 集合模拟 Redis 五种数据结构 + 底层编码切换（直接运行）
 * <ul>
 *   <li>String — SDS（简单动态字符串），支持 int/embstr/raw 编码</li>
 *   <li>List — 双向链表 / ziplist / quicklist</li>
 *   <li>Hash — ziplist / hashtable</li>
 *   <li>Set — intset / hashtable</li>
 *   <li>ZSet（Sorted Set）— ziplist / skiplist + hashtable</li>
 * </ul>
 *
 * <p>Part B：用 Jedis 连接真实 Redis 操作五种数据结构
 *
 * <p>Part B 运行前启动依赖：
 * {@code docker compose -f docker/docker-compose.yml up -d redis}
 *
 * <h3>Redis 数据结构与底层编码：</h3>
 * <pre>
 *  数据类型    底层编码（小数据量）    底层编码（大数据量）    切换条件
 *  String     int / embstr          raw                  长度 > 44 字节
 *  List       ziplist               quicklist            元素数 > 128 或单元素 > 64 字节
 *  Hash       ziplist               hashtable            字段数 > 128 或单值 > 64 字节
 *  Set        intset                hashtable            元素数 > 128 或含非整数元素
 *  ZSet       ziplist               skiplist+hashtable   元素数 > 128 或单值 > 64 字节
 * </pre>
 */
public class DataStructureDemo {

    // ==================== Part A：模拟 Redis 数据结构 ====================

    /** 模拟 Redis String（SDS） */
    static class SimulatedString {
        private Object value;       // int / String
        private String encoding;    // int / embstr / raw

        void set(String val) {
            try {
                long num = Long.parseLong(val);
                this.value = num;
                this.encoding = "int";
            } catch (NumberFormatException e) {
                this.value = val;
                this.encoding = val.length() <= 44 ? "embstr" : "raw";
            }
        }

        String get() { return String.valueOf(value); }

        /** INCR 操作：只有 int 编码才能自增 */
        long incr() {
            if (!"int".equals(encoding)) throw new RuntimeException("ERR value is not an integer");
            long num = (Long) value + 1;
            this.value = num;
            return num;
        }

        /** APPEND 操作：追加后编码可能从 embstr 变为 raw */
        int append(String suffix) {
            String current = String.valueOf(value);
            String newVal = current + suffix;
            this.value = newVal;
            this.encoding = newVal.length() <= 44 ? "embstr" : "raw";
            return newVal.length();
        }

        String getEncoding() { return encoding; }
    }

    /** 模拟 Redis Hash（ziplist → hashtable 编码切换） */
    static class SimulatedHash {
        private final java.util.LinkedHashMap<String, String> data = new java.util.LinkedHashMap<>();
        private String encoding = "ziplist";
        private static final int ZIPLIST_MAX_ENTRIES = 128;
        private static final int ZIPLIST_MAX_VALUE_LEN = 64;

        void hset(String field, String value) {
            data.put(field, value);
            checkEncoding(value);
        }

        String hget(String field) { return data.get(field); }

        java.util.Map<String, String> hgetall() { return new java.util.LinkedHashMap<>(data); }

        int hlen() { return data.size(); }

        private void checkEncoding(String value) {
            if (data.size() > ZIPLIST_MAX_ENTRIES || value.length() > ZIPLIST_MAX_VALUE_LEN) {
                encoding = "hashtable";
            }
        }

        String getEncoding() { return encoding; }
    }

    /** 模拟 Redis Set（intset → hashtable 编码切换） */
    static class SimulatedSet {
        private final java.util.LinkedHashSet<String> data = new java.util.LinkedHashSet<>();
        private String encoding = "intset";
        private boolean allIntegers = true;

        void sadd(String... members) {
            for (String m : members) {
                data.add(m);
                if (allIntegers) {
                    try { Long.parseLong(m); } catch (NumberFormatException e) { allIntegers = false; }
                }
            }
            checkEncoding();
        }

        boolean sismember(String member) { return data.contains(member); }

        java.util.Set<String> smembers() { return new java.util.LinkedHashSet<>(data); }

        int scard() { return data.size(); }

        /** 集合运算 */
        java.util.Set<String> sinter(SimulatedSet other) {
            java.util.Set<String> result = new java.util.LinkedHashSet<>(data);
            result.retainAll(other.data);
            return result;
        }

        java.util.Set<String> sunion(SimulatedSet other) {
            java.util.Set<String> result = new java.util.LinkedHashSet<>(data);
            result.addAll(other.data);
            return result;
        }

        java.util.Set<String> sdiff(SimulatedSet other) {
            java.util.Set<String> result = new java.util.LinkedHashSet<>(data);
            result.removeAll(other.data);
            return result;
        }

        private void checkEncoding() {
            if (!allIntegers || data.size() > 128) {
                encoding = "hashtable";
            }
        }

        String getEncoding() { return encoding; }
    }

    /** 模拟 Redis ZSet（Sorted Set，skiplist + hashtable） */
    static class SimulatedZSet {
        // score → member 的有序映射（模拟 skiplist 的排序功能）
        private final java.util.TreeMap<Double, java.util.LinkedHashSet<String>> scoreMap = new java.util.TreeMap<>();
        // member → score 的映射（模拟 hashtable 的 O(1) 查找）
        private final java.util.LinkedHashMap<String, Double> memberMap = new java.util.LinkedHashMap<>();
        private String encoding = "ziplist";

        void zadd(double score, String member) {
            // 如果 member 已存在，先移除旧 score
            if (memberMap.containsKey(member)) {
                double oldScore = memberMap.get(member);
                scoreMap.get(oldScore).remove(member);
                if (scoreMap.get(oldScore).isEmpty()) scoreMap.remove(oldScore);
            }
            memberMap.put(member, score);
            scoreMap.computeIfAbsent(score, k -> new java.util.LinkedHashSet<>()).add(member);
            if (memberMap.size() > 128) encoding = "skiplist";
        }

        Double zscore(String member) { return memberMap.get(member); }

        /** ZRANK：返回成员的排名（从 0 开始，按 score 升序） */
        int zrank(String member) {
            if (!memberMap.containsKey(member)) return -1;
            int rank = 0;
            for (var entry : scoreMap.entrySet()) {
                for (String m : entry.getValue()) {
                    if (m.equals(member)) return rank;
                    rank++;
                }
            }
            return -1;
        }

        /** ZRANGE：按排名范围获取成员 */
        java.util.List<String> zrange(int start, int stop) {
            java.util.List<String> all = new java.util.ArrayList<>();
            for (var entry : scoreMap.entrySet()) {
                all.addAll(entry.getValue());
            }
            if (stop < 0) stop = all.size() + stop;
            return all.subList(Math.max(0, start), Math.min(all.size(), stop + 1));
        }

        /** ZRANGEBYSCORE：按分数范围获取成员 */
        java.util.List<String> zrangeByScore(double min, double max) {
            java.util.List<String> result = new java.util.ArrayList<>();
            for (var entry : scoreMap.subMap(min, true, max, true).entrySet()) {
                result.addAll(entry.getValue());
            }
            return result;
        }

        int zcard() { return memberMap.size(); }
        String getEncoding() { return encoding; }
    }

    // ==================== Part A 演示方法 ====================

    /** 演示1：String 类型 + 编码切换 */
    static void demoString() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示1：String — SDS + 编码切换");
        System.out.println("═══════════════════════════════════════════════════");

        SimulatedString s1 = new SimulatedString();
        s1.set("12345");
        System.out.printf("\n  SET key 12345 → encoding=%s（纯数字用 int 编码）%n", s1.getEncoding());
        System.out.printf("  INCR key → %d%n", s1.incr());

        SimulatedString s2 = new SimulatedString();
        s2.set("hello");
        System.out.printf("  SET key hello → encoding=%s（短字符串用 embstr）%n", s2.getEncoding());

        SimulatedString s3 = new SimulatedString();
        s3.set("a]".repeat(30));
        System.out.printf("  SET key (60字节) → encoding=%s（长字符串用 raw）%n", s3.getEncoding());

        // APPEND 导致编码切换
        SimulatedString s4 = new SimulatedString();
        s4.set("short");
        System.out.printf("\n  SET key short → encoding=%s%n", s4.getEncoding());
        s4.append("_" + "x".repeat(50));
        System.out.printf("  APPEND 后 → encoding=%s（超过 44 字节，embstr → raw）%n", s4.getEncoding());
        System.out.println();
    }

    /** 演示2：Hash 类型 + 编码切换 */
    static void demoHash() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示2：Hash — ziplist → hashtable 编码切换");
        System.out.println("═══════════════════════════════════════════════════");

        SimulatedHash hash = new SimulatedHash();
        hash.hset("name", "张三");
        hash.hset("age", "25");
        hash.hset("city", "北京");
        System.out.printf("\n  HSET 3 个字段 → encoding=%s（字段少用 ziplist）%n", hash.getEncoding());

        // 添加超过 128 个字段 → 切换为 hashtable
        for (int i = 0; i < 130; i++) {
            hash.hset("field-" + i, "value-" + i);
        }
        System.out.printf("  HSET 130+ 个字段 → encoding=%s（超过阈值，切换为 hashtable）%n", hash.getEncoding());
        System.out.printf("  HLEN = %d%n", hash.hlen());
        System.out.println();
    }

    /** 演示3：Set 类型 + 集合运算 */
    static void demoSet() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示3：Set — intset → hashtable + 集合运算");
        System.out.println("═══════════════════════════════════════════════════");

        SimulatedSet set1 = new SimulatedSet();
        set1.sadd("1", "2", "3", "4", "5");
        System.out.printf("\n  SADD 纯整数 → encoding=%s%n", set1.getEncoding());

        set1.sadd("hello");
        System.out.printf("  SADD 非整数 → encoding=%s（含非整数，intset → hashtable）%n", set1.getEncoding());

        // 集合运算
        SimulatedSet setA = new SimulatedSet();
        setA.sadd("Java", "Python", "Go", "Rust");
        SimulatedSet setB = new SimulatedSet();
        setB.sadd("Java", "Go", "C++", "Kotlin");

        System.out.println("\n  集合运算：");
        System.out.printf("    A = %s%n", setA.smembers());
        System.out.printf("    B = %s%n", setB.smembers());
        System.out.printf("    SINTER (交集) = %s%n", setA.sinter(setB));
        System.out.printf("    SUNION (并集) = %s%n", setA.sunion(setB));
        System.out.printf("    SDIFF  (差集 A-B) = %s%n", setA.sdiff(setB));

        System.out.println("\n  应用场景：共同关注、共同好友、标签系统");
        System.out.println();
    }

    /** 演示4：ZSet 类型 — 排行榜 */
    static void demoZSet() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示4：ZSet（Sorted Set）— 排行榜");
        System.out.println("═══════════════════════════════════════════════════");

        SimulatedZSet leaderboard = new SimulatedZSet();
        leaderboard.zadd(95.5, "张三");
        leaderboard.zadd(88.0, "李四");
        leaderboard.zadd(92.0, "王五");
        leaderboard.zadd(78.5, "赵六");
        leaderboard.zadd(99.0, "孙七");

        System.out.println("\n  成绩排行榜：");
        System.out.printf("    ZADD 5 个成员 → encoding=%s%n", leaderboard.getEncoding());

        // ZRANGE：按排名获取（升序）
        System.out.printf("    ZRANGE 0 -1 (升序): %s%n", leaderboard.zrange(0, -1));

        // ZSCORE + ZRANK
        System.out.printf("    ZSCORE 张三 = %.1f%n", leaderboard.zscore("张三"));
        System.out.printf("    ZRANK 张三 = %d（排名从 0 开始）%n", leaderboard.zrank("张三"));

        // ZRANGEBYSCORE：按分数范围
        System.out.printf("    ZRANGEBYSCORE 85 95 = %s%n", leaderboard.zrangeByScore(85, 95));

        // 更新分数
        leaderboard.zadd(100.0, "张三");
        System.out.printf("    ZADD 张三 100（更新分数）→ ZRANGE: %s%n", leaderboard.zrange(0, -1));

        System.out.println("\n  应用场景：排行榜、延迟队列（score=时间戳）、带权重的标签");
        System.out.println();
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  Redis 五种数据结构演示（混合模式）                     ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        // ===== Part A：纯内存模拟 =====
        System.out.println("══════════ Part A：模拟 Redis 数据结构 ══════════");
        System.out.println();
        demoString();
        demoHash();
        demoSet();
        demoZSet();

        // ===== Part B：连接真实 Redis =====
        if (args.length > 0 && "real".equals(args[0])) {
            System.out.println("══════════ Part B：Jedis 操作真实 Redis ══════════");
            System.out.println();
            RealRedis.run();
        } else {
            System.out.println("提示：运行 Part B（真实 Redis）请传入参数 'real'");
            System.out.println("  启动 Redis: docker compose -f docker/docker-compose.yml up -d redis");
        }
    }

    // ==================== Part B：真实 Redis ====================

    /**
     * Part B：用 Jedis 连接真实 Redis 操作五种数据结构。
     * 启动 Redis：docker compose -f docker/docker-compose.yml up -d redis
     * 默认地址：localhost:6379，无密码
     */
    static class RealRedis {

        static final String REDIS_HOST = "localhost";
        static final int REDIS_PORT = 6379;

        static void run() throws Exception {
            redis.clients.jedis.Jedis jedis = new redis.clients.jedis.Jedis(REDIS_HOST, REDIS_PORT);

            try {
                System.out.println("  连接 Redis: " + jedis.ping());

                demoRealString(jedis);
                demoRealHash(jedis);
                demoRealSet(jedis);
                demoRealZSet(jedis);
                demoRealList(jedis);
            } finally {
                // 清理测试数据（如需保留在 Redis 中查看，注释掉 cleanup 调用即可）
                cleanup(jedis);
                jedis.close();
            }
        }

        static void demoRealString(redis.clients.jedis.Jedis jedis) {
            System.out.println("\n  【String 操作】");
            jedis.set("demo:name", "张三");
            jedis.set("demo:counter", "100");

            System.out.printf("    GET demo:name = %s%n", jedis.get("demo:name"));
            System.out.printf("    INCR demo:counter = %d%n", jedis.incr("demo:counter"));
            System.out.printf("    OBJECT ENCODING demo:counter = %s%n", jedis.objectEncoding("demo:counter"));
            System.out.printf("    OBJECT ENCODING demo:name = %s%n", jedis.objectEncoding("demo:name"));

            // SETNX（分布式锁基础）
            long nx1 = jedis.setnx("demo:lock", "holder-1");
            long nx2 = jedis.setnx("demo:lock", "holder-2");
            System.out.printf("    SETNX demo:lock holder-1 = %d（成功）%n", nx1);
            System.out.printf("    SETNX demo:lock holder-2 = %d（失败，已存在）%n", nx2);
        }

        static void demoRealHash(redis.clients.jedis.Jedis jedis) {
            System.out.println("\n  【Hash 操作】");
            jedis.hset("demo:user:1", "name", "张三");
            jedis.hset("demo:user:1", "age", "25");
            jedis.hset("demo:user:1", "city", "北京");

            System.out.printf("    HGET demo:user:1 name = %s%n", jedis.hget("demo:user:1", "name"));
            System.out.printf("    HGETALL = %s%n", jedis.hgetAll("demo:user:1"));
            System.out.printf("    HLEN = %d%n", jedis.hlen("demo:user:1"));
            System.out.printf("    OBJECT ENCODING = %s%n", jedis.objectEncoding("demo:user:1"));

            jedis.hincrBy("demo:user:1", "age", 1);
            System.out.printf("    HINCRBY age 1 → age=%s%n", jedis.hget("demo:user:1", "age"));
        }

        static void demoRealSet(redis.clients.jedis.Jedis jedis) {
            System.out.println("\n  【Set 操作 + 集合运算】");
            jedis.sadd("demo:langs:a", "Java", "Python", "Go", "Rust");
            jedis.sadd("demo:langs:b", "Java", "Go", "C++", "Kotlin");

            System.out.printf("    SMEMBERS A = %s%n", jedis.smembers("demo:langs:a"));
            System.out.printf("    SMEMBERS B = %s%n", jedis.smembers("demo:langs:b"));
            System.out.printf("    SINTER (交集) = %s%n", jedis.sinter("demo:langs:a", "demo:langs:b"));
            System.out.printf("    SUNION (并集) = %s%n", jedis.sunion("demo:langs:a", "demo:langs:b"));
            System.out.printf("    SDIFF  (A-B)  = %s%n", jedis.sdiff("demo:langs:a", "demo:langs:b"));
            System.out.printf("    SISMEMBER A Java = %s%n", jedis.sismember("demo:langs:a", "Java"));
        }

        static void demoRealZSet(redis.clients.jedis.Jedis jedis) {
            System.out.println("\n  【ZSet 操作 — 排行榜】");
            jedis.zadd("demo:rank", 95.5, "张三");
            jedis.zadd("demo:rank", 88.0, "李四");
            jedis.zadd("demo:rank", 92.0, "王五");
            jedis.zadd("demo:rank", 99.0, "孙七");

            System.out.printf("    ZRANGE 0 -1 WITHSCORES (升序):%n");
            for (redis.clients.jedis.resps.Tuple t : jedis.zrangeWithScores("demo:rank", 0, -1)) {
                System.out.printf("      %s: %.1f%n", t.getElement(), t.getScore());
            }
            System.out.printf("    ZREVRANGE 0 0 (第一名) = %s%n", jedis.zrevrange("demo:rank", 0, 0));
            System.out.printf("    ZSCORE 张三 = %.1f%n", jedis.zscore("demo:rank", "张三"));
            System.out.printf("    ZRANK 张三 = %d%n", jedis.zrank("demo:rank", "张三"));
        }

        static void demoRealList(redis.clients.jedis.Jedis jedis) {
            System.out.println("\n  【List 操作 — 消息队列】");
            jedis.rpush("demo:queue", "任务1", "任务2", "任务3");
            System.out.printf("    RPUSH 3 个任务, LLEN = %d%n", jedis.llen("demo:queue"));
            System.out.printf("    LPOP = %s%n", jedis.lpop("demo:queue"));
            System.out.printf("    LPOP = %s%n", jedis.lpop("demo:queue"));
            System.out.printf("    LLEN = %d%n", jedis.llen("demo:queue"));
        }

        /** 清理测试数据。如需保留数据在 Redis 中查看，注释掉 cleanup() 调用即可 */
        static void cleanup(redis.clients.jedis.Jedis jedis) {
            String[] keys = {"demo:name", "demo:counter", "demo:lock",
                    "demo:user:1", "demo:langs:a", "demo:langs:b",
                    "demo:rank", "demo:queue"};
            jedis.del(keys);
            System.out.println("\n  清理：已删除测试数据");
            System.out.println("  提示：如需保留数据，注释掉 cleanup() 调用即可");
        }
    }
}
