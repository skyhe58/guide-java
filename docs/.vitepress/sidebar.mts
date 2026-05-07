import type { DefaultTheme } from 'vitepress'

/**
 * 侧边栏配置
 * 按五层学习架构组织：语言基础 → 语言深入 → 框架应用 → 分布式体系 → 综合进阶
 */
export const sidebar: DefaultTheme.Sidebar = {
  // ==================== 指南 ====================
  '/guide/': [
    {
      text: '开始',
      items: [
        { text: '快速开始', link: '/guide/getting-started' },
        { text: '使用指南', link: '/guide/how-to-use' },
      ],
    },
  ],

  // ==================== 第一层：语言基础 ====================
  '/1-java-core/1.1-java-basics/': [
    {
      text: 'Java 基础',
      items: [
        { text: '数据类型与包装类', link: '/1-java-core/1.1-java-basics/01-data-types' },
        { text: '值传递与引用传递', link: '/1-java-core/1.1-java-basics/02-value-passing' },
        { text: 'String 深入', link: '/1-java-core/1.1-java-basics/03-string-deep-dive' },
        { text: '面向对象', link: '/1-java-core/1.1-java-basics/04-oop' },
        { text: '集合框架', link: '/1-java-core/1.1-java-basics/05-collections' },
        { text: '异常处理', link: '/1-java-core/1.1-java-basics/06-exceptions' },
        { text: '泛型', link: '/1-java-core/1.1-java-basics/07-generics' },
        { text: '反射', link: '/1-java-core/1.1-java-basics/08-reflection' },
        { text: '注解', link: '/1-java-core/1.1-java-basics/09-annotations' },
        { text: 'IO 与 NIO', link: '/1-java-core/1.1-java-basics/10-io-streams' },
        { text: 'Lambda 与 Stream', link: '/1-java-core/1.1-java-basics/11-lambda-stream' },
        { text: 'JDK 版本特性', link: '/1-java-core/1.1-java-basics/12-new-features' },
        { text: '面试指南', link: '/1-java-core/1.1-java-basics/99-interview' },
      ],
    },
  ],

  // ==================== 第二层：语言深入 ====================
  '/1-java-core/1.2-java-advanced/': [
    {
      text: 'Java 进阶',
      items: [
        { text: '集合源码分析', link: '/1-java-core/1.2-java-advanced/01-collections-source' },
        { text: '类加载机制', link: '/1-java-core/1.2-java-advanced/02-classloader' },
        { text: '动态代理', link: '/1-java-core/1.2-java-advanced/03-dynamic-proxy' },
        { text: 'SPI 机制', link: '/1-java-core/1.2-java-advanced/04-spi' },
        { text: '序列化机制', link: '/1-java-core/1.2-java-advanced/05-serialization' },
        { text: '网络编程', link: '/1-java-core/1.2-java-advanced/06-network-programming' },
        { text: 'JMM 与 happens-before', link: '/1-java-core/1.2-java-advanced/07-jmm' },
        { text: '面试指南', link: '/1-java-core/1.2-java-advanced/99-interview' },
      ],
    },
  ],

  '/1-java-core/1.3-concurrent/': [
    {
      text: '并发编程',
      items: [
        { text: '线程生命周期', link: '/1-java-core/1.3-concurrent/01-thread-lifecycle' },
        { text: 'synchronized 原理', link: '/1-java-core/1.3-concurrent/02-synchronized' },
        { text: 'ReentrantLock 与 AQS', link: '/1-java-core/1.3-concurrent/03-reentrantlock-aqs' },
        { text: 'volatile 原理', link: '/1-java-core/1.3-concurrent/04-volatile' },
        { text: '线程池', link: '/1-java-core/1.3-concurrent/05-thread-pool' },
        { text: '并发工具类', link: '/1-java-core/1.3-concurrent/06-concurrent-tools' },
        { text: 'ThreadLocal', link: '/1-java-core/1.3-concurrent/07-threadlocal' },
        { text: 'CompletableFuture', link: '/1-java-core/1.3-concurrent/08-completable-future' },
        { text: 'CAS 与原子类', link: '/1-java-core/1.3-concurrent/09-cas-atomic' },
        { text: '死锁检测与避免', link: '/1-java-core/1.3-concurrent/10-deadlock' },
        { text: '面试指南', link: '/1-java-core/1.3-concurrent/99-interview' },
      ],
    },
  ],

  '/1-java-core/1.4-jvm/': [
    {
      text: 'JVM',
      items: [
        { text: '内存模型与区域', link: '/1-java-core/1.4-jvm/01-memory-model' },
        { text: 'GC 算法与收集器', link: '/1-java-core/1.4-jvm/02-gc' },
        { text: '类加载过程', link: '/1-java-core/1.4-jvm/03-classloading' },
        { text: 'JIT 编译', link: '/1-java-core/1.4-jvm/04-jit' },
        { text: 'JVM 调优', link: '/1-java-core/1.4-jvm/05-tuning' },
        { text: '诊断工具', link: '/1-java-core/1.4-jvm/06-diagnostic' },
        { text: '面试指南', link: '/1-java-core/1.4-jvm/99-interview' },
      ],
    },
  ],

  '/1-java-core/1.5-design-patterns/': [
    {
      text: '设计模式',
      items: [
        { text: '创建型模式', link: '/1-java-core/1.5-design-patterns/01-creational' },
        { text: '结构型模式', link: '/1-java-core/1.5-design-patterns/02-structural' },
        { text: '行为型模式', link: '/1-java-core/1.5-design-patterns/03-behavioral' },
        { text: 'Spring 中的设计模式', link: '/1-java-core/1.5-design-patterns/04-spring-patterns' },
        { text: '设计原则', link: '/1-java-core/1.5-design-patterns/05-principles' },
        { text: '面试指南', link: '/1-java-core/1.5-design-patterns/99-interview' },
      ],
    },
  ],

  '/1-java-core/1.6-algorithm/': [
    {
      text: '数据结构与算法',
      items: [
        { text: '链表', link: '/1-java-core/1.6-algorithm/01-linked-list' },
        { text: '栈与队列', link: '/1-java-core/1.6-algorithm/02-stack-queue' },
        { text: '哈希表', link: '/1-java-core/1.6-algorithm/03-hash-table' },
        { text: '二叉树', link: '/1-java-core/1.6-algorithm/04-binary-tree' },
        { text: '堆', link: '/1-java-core/1.6-algorithm/05-heap' },
        { text: '排序算法', link: '/1-java-core/1.6-algorithm/06-sorting' },
        { text: '二分查找', link: '/1-java-core/1.6-algorithm/07-binary-search' },
        { text: '双指针与滑动窗口', link: '/1-java-core/1.6-algorithm/08-two-pointers' },
        { text: '动态规划', link: '/1-java-core/1.6-algorithm/09-dynamic-programming' },
        { text: '回溯算法', link: '/1-java-core/1.6-algorithm/10-backtracking' },
        { text: '面试指南', link: '/1-java-core/1.6-algorithm/99-interview' },
      ],
    },
  ],

  // ==================== 第三层：框架应用 ====================
  '/2-framework/2.1-network/': [
    {
      text: '网络与协议',
      items: [
        { text: 'TCP/IP 协议栈', link: '/2-framework/2.1-network/01-tcp-ip' },
        { text: 'HTTP 协议', link: '/2-framework/2.1-network/02-http' },
        { text: 'WebSocket', link: '/2-framework/2.1-network/03-websocket' },
        { text: 'DNS 与 CDN', link: '/2-framework/2.1-network/04-dns-cdn' },
        { text: '网络安全', link: '/2-framework/2.1-network/05-security' },
        { text: 'RESTful API', link: '/2-framework/2.1-network/06-restful' },
        { text: 'RPC 框架', link: '/2-framework/2.1-network/07-rpc' },
        { text: '面试指南', link: '/2-framework/2.1-network/99-interview' },
      ],
    },
  ],

  '/2-framework/2.2-springboot/': [
    {
      text: 'Spring Boot',
      items: [
        { text: 'IoC 与依赖注入', link: '/2-framework/2.2-springboot/01-ioc-di' },
        { text: 'AOP 原理', link: '/2-framework/2.2-springboot/02-aop' },
        { text: '循环依赖', link: '/2-framework/2.2-springboot/03-circular-dependency' },
        { text: '启动流程', link: '/2-framework/2.2-springboot/04-startup' },
        { text: 'Starter 机制', link: '/2-framework/2.2-springboot/05-starter' },
        { text: '配置文件体系', link: '/2-framework/2.2-springboot/06-config-files' },
        { text: 'Web 开发', link: '/2-framework/2.2-springboot/07-web' },
        { text: '数据访问', link: '/2-framework/2.2-springboot/08-data-access' },
        { text: 'Spring Security', link: '/2-framework/2.2-springboot/09-security' },
        { text: '日志体系', link: '/2-framework/2.2-springboot/10-logging' },
        { text: '缓存集成', link: '/2-framework/2.2-springboot/11-cache' },
        { text: '定时任务', link: '/2-framework/2.2-springboot/12-task' },
        { text: 'Actuator 监控', link: '/2-framework/2.2-springboot/13-actuator' },
        { text: '面试指南', link: '/2-framework/2.2-springboot/99-interview' },
      ],
    },
  ],

  '/2-framework/2.3-springcloud/': [
    {
      text: 'Spring Cloud',
      items: [
        { text: '🚀 实战项目总览', link: '/2-framework/2.3-springcloud/00-spring-cloud-project' },
        { text: '服务注册与发现', link: '/2-framework/2.3-springcloud/01-registry' },
        { text: '负载均衡', link: '/2-framework/2.3-springcloud/02-loadbalancer' },
        { text: 'OpenFeign', link: '/2-framework/2.3-springcloud/03-feign' },
        { text: '熔断降级', link: '/2-framework/2.3-springcloud/04-circuit-breaker' },
        { text: 'Gateway 网关', link: '/2-framework/2.3-springcloud/05-gateway' },
        { text: '配置中心', link: '/2-framework/2.3-springcloud/06-config' },
        { text: '链路追踪', link: '/2-framework/2.3-springcloud/07-tracing' },
        { text: '分布式事务', link: '/2-framework/2.3-springcloud/08-transaction' },
        { text: '版本兼容性', link: '/2-framework/2.3-springcloud/09-version-compatibility' },
        { text: '面试指南', link: '/2-framework/2.3-springcloud/99-interview' },
      ],
    },
  ],

  // ==================== 数据存储 ====================
  '/3-data-store/3.1-database/': [
    {
      text: '数据库',
      items: [
        { text: '索引原理', link: '/3-data-store/3.1-database/01-index-theory' },
        { text: '事务与隔离级别', link: '/3-data-store/3.1-database/02-transaction' },
        { text: '锁机制', link: '/3-data-store/3.1-database/03-lock' },
        { text: 'SQL 优化', link: '/3-data-store/3.1-database/04-optimization' },
        { text: '分库分表', link: '/3-data-store/3.1-database/05-sharding' },
        { text: 'Binlog 与主从同步', link: '/3-data-store/3.1-database/06-binlog' },
        { text: 'Redo/Undo Log', link: '/3-data-store/3.1-database/07-log-system' },
        { text: '高可用方案', link: '/3-data-store/3.1-database/08-high-availability' },
        { text: '分布式 ID', link: '/3-data-store/3.1-database/09-distributed-id' },
        { text: '连接池', link: '/3-data-store/3.1-database/10-pool' },
        { text: '面试指南', link: '/3-data-store/3.1-database/99-interview' },
      ],
    },
  ],

  '/3-data-store/3.2-redis/': [
    {
      text: 'Redis',
      items: [
        { text: '数据结构', link: '/3-data-store/3.2-redis/01-data-structures' },
        { text: '持久化机制', link: '/3-data-store/3.2-redis/02-persistence' },
        { text: '主从与集群', link: '/3-data-store/3.2-redis/03-replication' },
        { text: '缓存问题', link: '/3-data-store/3.2-redis/04-cache-problems' },
        { text: '分布式锁', link: '/3-data-store/3.2-redis/05-distributed-lock' },
        { text: 'Spring Boot 集成', link: '/3-data-store/3.2-redis/06-spring-integration' },
        { text: '面试指南', link: '/3-data-store/3.2-redis/99-interview' },
      ],
    },
  ],

  '/3-data-store/3.3-elasticsearch/': [
    {
      text: 'Elasticsearch',
      items: [
        { text: '倒排索引', link: '/3-data-store/3.3-elasticsearch/01-inverted-index' },
        { text: '映射与分析器', link: '/3-data-store/3.3-elasticsearch/02-mapping' },
        { text: 'CRUD 操作', link: '/3-data-store/3.3-elasticsearch/03-crud' },
        { text: 'DSL 查询', link: '/3-data-store/3.3-elasticsearch/04-dsl-query' },
        { text: '聚合分析', link: '/3-data-store/3.3-elasticsearch/05-aggregation' },
        { text: 'Spring Data ES', link: '/3-data-store/3.3-elasticsearch/06-spring-data' },
        { text: '面试指南', link: '/3-data-store/3.3-elasticsearch/99-interview' },
      ],
    },
  ],

  '/3-data-store/3.4-mongodb/': [
    {
      text: 'MongoDB',
      items: [
        { text: '文档模型', link: '/3-data-store/3.4-mongodb/01-document-model' },
        { text: 'CRUD 操作', link: '/3-data-store/3.4-mongodb/02-crud' },
        { text: '聚合管道', link: '/3-data-store/3.4-mongodb/03-aggregation' },
        { text: '索引', link: '/3-data-store/3.4-mongodb/04-index' },
        { text: 'Spring Data MongoDB', link: '/3-data-store/3.4-mongodb/05-spring-data' },
        { text: '面试指南', link: '/3-data-store/3.4-mongodb/99-interview' },
      ],
    },
  ],

  '/3-data-store/3.5-minio/': [
    {
      text: 'MinIO',
      items: [
        { text: '架构原理', link: '/3-data-store/3.5-minio/01-architecture' },
        { text: '桶管理', link: '/3-data-store/3.5-minio/02-bucket-management' },
        { text: '文件操作', link: '/3-data-store/3.5-minio/03-file-operations' },
        { text: '面试指南', link: '/3-data-store/3.5-minio/99-interview' },
      ],
    },
  ],

  // ==================== 中间件 ====================
  '/4-middleware/4.1-mq-rabbitmq/': [
    {
      text: 'RabbitMQ',
      items: [
        { text: 'RabbitMQ 核心概念', link: '/4-middleware/4.1-mq-rabbitmq/01-rabbitmq' },
        { text: '消息可靠性', link: '/4-middleware/4.1-mq-rabbitmq/02-rabbitmq-reliability' },
        { text: '高级特性', link: '/4-middleware/4.1-mq-rabbitmq/03-rabbitmq-advanced' },
        { text: 'Spring Boot 集成', link: '/4-middleware/4.1-mq-rabbitmq/04-rabbitmq-spring' },
        { text: '面试指南', link: '/4-middleware/4.1-mq-rabbitmq/99-interview' },
      ],
    },
  ],

  '/4-middleware/4.2-mq-kafka/': [
    {
      text: 'Kafka',
      items: [
        { text: 'Kafka 架构与原理', link: '/4-middleware/4.2-mq-kafka/01-kafka' },
        { text: '消息可靠性', link: '/4-middleware/4.2-mq-kafka/02-kafka-reliability' },
        { text: '高级特性', link: '/4-middleware/4.2-mq-kafka/03-kafka-advanced' },
        { text: 'Spring Boot 集成', link: '/4-middleware/4.2-mq-kafka/04-kafka-spring' },
        { text: '面试指南', link: '/4-middleware/4.2-mq-kafka/99-interview' },
      ],
    },
  ],

  '/4-middleware/4.3-mq-mqtt/': [
    {
      text: 'MQTT',
      items: [
        { text: 'MQTT 协议原理', link: '/4-middleware/4.3-mq-mqtt/01-mqtt-protocol' },
        { text: 'Broker 部署', link: '/4-middleware/4.3-mq-mqtt/02-mqtt-broker' },
        { text: 'Spring Boot 集成', link: '/4-middleware/4.3-mq-mqtt/03-mqtt-spring' },
        { text: '面试指南', link: '/4-middleware/4.3-mq-mqtt/99-interview' },
      ],
    },
  ],

  '/4-middleware/4.4-config-center/': [
    {
      text: '配置中心',
      items: [
        { text: 'Apollo', link: '/4-middleware/4.4-config-center/01-apollo' },
        { text: 'Nacos Config', link: '/4-middleware/4.4-config-center/02-nacos-config' },
        { text: '选型对比', link: '/4-middleware/4.4-config-center/03-comparison' },
        { text: '配置安全', link: '/4-middleware/4.4-config-center/04-security' },
        { text: '面试指南', link: '/4-middleware/4.4-config-center/99-interview' },
      ],
    },
  ],

  '/4-middleware/4.5-registry/': [
    {
      text: '注册中心',
      items: [
        { text: '服务注册与发现原理', link: '/4-middleware/4.5-registry/01-principles' },
        { text: 'Consul', link: '/4-middleware/4.5-registry/02-consul' },
        { text: 'Zookeeper', link: '/4-middleware/4.5-registry/03-zookeeper' },
        { text: 'Nacos', link: '/4-middleware/4.5-registry/04-nacos' },
        { text: '选型对比', link: '/4-middleware/4.5-registry/05-comparison' },
        { text: '面试指南', link: '/4-middleware/4.5-registry/99-interview' },
      ],
    },
  ],

  '/4-middleware/4.6-nginx/': [
    {
      text: 'Nginx',
      items: [
        { text: '架构原理', link: '/4-middleware/4.6-nginx/01-architecture' },
        { text: '反向代理', link: '/4-middleware/4.6-nginx/02-reverse-proxy' },
        { text: '负载均衡', link: '/4-middleware/4.6-nginx/03-load-balance' },
        { text: 'HTTPS 配置', link: '/4-middleware/4.6-nginx/04-https' },
        { text: '限流防刷', link: '/4-middleware/4.6-nginx/05-rate-limit' },
        { text: '跨域配置', link: '/4-middleware/4.6-nginx/06-cors' },
        { text: '进阶主题', link: '/4-middleware/4.6-nginx/07-advanced' },
        { text: '面试指南', link: '/4-middleware/4.6-nginx/99-interview' },
      ],
    },
  ],

  // ==================== 分布式 ====================
  '/5-distributed/5.1-distributed/': [
    {
      text: '分布式系统',
      items: [
        { text: 'CAP 与 BASE', link: '/5-distributed/5.1-distributed/01-cap-base' },
        { text: '一致性算法', link: '/5-distributed/5.1-distributed/02-consensus' },
        { text: '分布式锁', link: '/5-distributed/5.1-distributed/03-distributed-lock' },
        { text: '分布式事务', link: '/5-distributed/5.1-distributed/04-distributed-transaction' },
        { text: '幂等性设计', link: '/5-distributed/5.1-distributed/05-idempotent' },
        { text: '限流算法', link: '/5-distributed/5.1-distributed/06-rate-limiting' },
        { text: '面试指南', link: '/5-distributed/5.1-distributed/99-interview' },
      ],
    },
  ],

  // ==================== DevOps ====================
  '/6-devops/6.1-docker-k8s/': [
    {
      text: 'Docker 与 K8s',
      items: [
        { text: 'Docker 基础', link: '/6-devops/6.1-docker-k8s/01-docker-basics' },
        { text: 'Dockerfile', link: '/6-devops/6.1-docker-k8s/02-dockerfile' },
        { text: 'Docker 网络', link: '/6-devops/6.1-docker-k8s/03-docker-network' },
        { text: 'Docker Compose', link: '/6-devops/6.1-docker-k8s/04-docker-compose' },
        { text: 'Java 容器化', link: '/6-devops/6.1-docker-k8s/05-java-docker' },
        { text: 'K8s 架构', link: '/6-devops/6.1-docker-k8s/06-k8s-architecture' },
        { text: 'K8s 资源对象', link: '/6-devops/6.1-docker-k8s/07-k8s-resources' },
        { text: '健康检查', link: '/6-devops/6.1-docker-k8s/08-k8s-health' },
        { text: '部署策略', link: '/6-devops/6.1-docker-k8s/09-k8s-deploy' },
        { text: 'HPA 自动扩缩', link: '/6-devops/6.1-docker-k8s/10-k8s-hpa' },
        { text: 'Helm', link: '/6-devops/6.1-docker-k8s/11-helm' },
        { text: '命令速查', link: '/6-devops/6.1-docker-k8s/12-cheatsheet' },
        { text: '面试指南', link: '/6-devops/6.1-docker-k8s/99-interview' },
      ],
    },
  ],

  '/6-devops/6.2-cicd/': [
    {
      text: 'CI/CD',
      items: [
        { text: 'Jenkins', link: '/6-devops/6.2-cicd/01-jenkins' },
        { text: 'GitHub Actions', link: '/6-devops/6.2-cicd/02-github-actions' },
        { text: 'GitLab CI', link: '/6-devops/6.2-cicd/03-gitlab-ci' },
        { text: '最佳实践', link: '/6-devops/6.2-cicd/04-best-practices' },
        { text: '面试指南', link: '/6-devops/6.2-cicd/99-interview' },
      ],
    },
  ],

  '/6-devops/6.3-monitoring/': [
    {
      text: '监控体系',
      items: [
        { text: 'Prometheus', link: '/6-devops/6.3-monitoring/01-prometheus' },
        { text: 'Grafana', link: '/6-devops/6.3-monitoring/02-grafana' },
        { text: 'Micrometer', link: '/6-devops/6.3-monitoring/03-micrometer' },
        { text: '日志监控', link: '/6-devops/6.3-monitoring/04-log-monitoring' },
        { text: '面试指南', link: '/6-devops/6.3-monitoring/99-interview' },
      ],
    },
  ],

  '/6-devops/6.4-linux/': [
    {
      text: 'Linux 运维',
      items: [
        { text: '常用命令', link: '/6-devops/6.4-linux/01-commands' },
        { text: 'Shell 脚本', link: '/6-devops/6.4-linux/02-shell' },
        { text: '性能排查', link: '/6-devops/6.4-linux/03-performance' },
        { text: '日志分析', link: '/6-devops/6.4-linux/04-log-analysis' },
        { text: 'JVM 问题排查', link: '/6-devops/6.4-linux/05-jvm-troubleshooting' },
        { text: '面试指南', link: '/6-devops/6.4-linux/99-interview' },
      ],
    },
  ],

  // ==================== AI 应用 ====================
  '/7-ai/7.1-ai/': [
    {
      text: 'AI 应用',
      items: [
        { text: 'Spring AI', link: '/7-ai/7.1-ai/01-spring-ai' },
        { text: 'LLM 集成', link: '/7-ai/7.1-ai/02-llm-integration' },
        { text: 'RAG 检索增强', link: '/7-ai/7.1-ai/03-rag' },
        { text: '向量数据库', link: '/7-ai/7.1-ai/04-vector-db' },
        { text: 'Prompt Engineering', link: '/7-ai/7.1-ai/05-prompt' },
        { text: 'AI Agent', link: '/7-ai/7.1-ai/06-agent' },
        { text: '框架对比', link: '/7-ai/7.1-ai/07-comparison' },
        { text: '面试指南', link: '/7-ai/7.1-ai/99-interview' },
      ],
    },
  ],

  // ==================== 架构设计 ====================
  '/8-architecture/': [
    {
      text: '架构设计场景',
      items: [
        { text: '秒杀系统', link: '/8-architecture/01-seckill' },
        { text: '短链接系统', link: '/8-architecture/02-short-url' },
        { text: '订单超时取消', link: '/8-architecture/03-order-timeout' },
        { text: '分布式缓存方案', link: '/8-architecture/04-cache-strategy' },
        { text: '接口幂等性', link: '/8-architecture/05-idempotent-design' },
        { text: '分布式 Session', link: '/8-architecture/06-distributed-session' },
        { text: '大文件上传', link: '/8-architecture/07-file-upload' },
        { text: '缓存与 DB 一致性', link: '/8-architecture/08-cache-db-consistency' },
        { text: '面试指南', link: '/8-architecture/99-interview' },
      ],
    },
  ],

  // ==================== 学习路径 ====================
  '/learning-paths/': [
    {
      text: '学习路径',
      items: [
        { text: 'Java 初学者', link: '/learning-paths/beginner' },
        { text: 'Java 中级进阶', link: '/learning-paths/intermediate' },
        { text: 'Java 高级深入', link: '/learning-paths/advanced' },
        { text: '面试突击', link: '/learning-paths/interview-sprint' },
        { text: '架构师成长', link: '/learning-paths/architect' },
      ],
    },
  ],

  // ==================== 面试汇总 ====================
  '/interview/': [
    {
      text: '面试汇总',
      items: [
        { text: '按公司类型', link: '/interview/by-company' },
        { text: '知识图谱', link: '/interview/knowledge-map' },
      ],
    },
  ],
}
