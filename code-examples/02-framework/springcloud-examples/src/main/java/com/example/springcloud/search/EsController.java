package com.example.springcloud.search;

import com.example.springcloud.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 10.A.20 Elasticsearch 全文搜索 Controller
 *
 * <p>演示 Spring Data Elasticsearch 的核心功能：
 * <ul>
 *   <li>文档的 CRUD 操作</li>
 *   <li>按标题全文搜索</li>
 * </ul>
 *
 * <h3>curl 测试命令：</h3>
 * <pre>
 * # 初始化测试数据
 * curl http://localhost:8090/demo/es/init
 *
 * # 全文搜索
 * curl "http://localhost:8090/demo/es/search?keyword=Spring"
 *
 * # 查询所有文章
 * curl http://localhost:8090/demo/es/articles
 *
 * # 创建文档
 * curl -X POST http://localhost:8090/demo/es/articles \
 *   -H "Content-Type: application/json" \
 *   -d '{"title":"测试文章","content":"测试内容","author":"test","tags":["test"]}'
 *
 * # 删除文档
 * curl -X DELETE http://localhost:8090/demo/es/articles/1
 * </pre>
 */
@RestController
@RequestMapping("/demo/es")
public class EsController {

    private static final Logger log = LoggerFactory.getLogger(EsController.class);

    private final ArticleRepository articleRepository;

    public EsController(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    // ==================== 初始化 ====================

    /**
     * 初始化测试数据
     *
     * <p>清空索引后批量插入 5 篇测试文章。
     *
     * @return 初始化结果
     */
    @GetMapping("/init")
    public Result<String> init() {
        log.info("[ES] 开始初始化测试数据");

        // 先清空所有数据
        articleRepository.deleteAll();

        // 批量插入测试文章
        List<ArticleDocument> articles = new ArrayList<>();
        articles.add(new ArticleDocument("1", "Java 并发编程实战",
                "深入理解 Java 多线程、锁机制、线程池等并发编程核心知识",
                "张三", List.of("Java", "并发", "多线程"), LocalDateTime.now()));
        articles.add(new ArticleDocument("2", "Spring Boot 快速入门",
                "从零开始学习 Spring Boot，掌握自动配置、Starter、Actuator 等核心特性",
                "李四", List.of("Spring", "Spring Boot", "微服务"), LocalDateTime.now()));
        articles.add(new ArticleDocument("3", "Redis 缓存设计与实战",
                "Redis 数据结构、缓存策略、分布式锁、集群部署等实战经验分享",
                "王五", List.of("Redis", "缓存", "NoSQL"), LocalDateTime.now()));
        articles.add(new ArticleDocument("4", "Kafka 消息队列深度解析",
                "Kafka 架构设计、分区策略、消费者组、Exactly-Once 语义等核心概念",
                "赵六", List.of("Kafka", "消息队列", "分布式"), LocalDateTime.now()));
        articles.add(new ArticleDocument("5", "Docker 容器化部署指南",
                "Docker 镜像构建、容器编排、Docker Compose、Kubernetes 入门",
                "孙七", List.of("Docker", "容器", "DevOps"), LocalDateTime.now()));

        articleRepository.saveAll(articles);
        log.info("[ES] 测试数据初始化完成，共 {} 篇文章", articles.size());

        return Result.ok("初始化完成，共插入 " + articles.size() + " 篇文章");
    }

    // ==================== 搜索与 CRUD ====================

    /**
     * 按标题全文搜索
     *
     * @param keyword 搜索关键词
     * @return 匹配的文章列表
     */
    @GetMapping("/search")
    public Result<List<ArticleDocument>> search(@RequestParam String keyword) {
        List<ArticleDocument> results = articleRepository.findByTitleContaining(keyword);
        log.info("[ES] 搜索关键词: {}, 命中 {} 篇文章", keyword, results.size());
        return Result.ok(results);
    }

    /**
     * 查询所有文章
     *
     * @return 文章列表
     */
    @GetMapping("/articles")
    public Result<List<ArticleDocument>> listAll() {
        List<ArticleDocument> articles = new ArrayList<>();
        articleRepository.findAll().forEach(articles::add);
        log.info("[ES] 查询所有文章，共 {} 篇", articles.size());
        return Result.ok(articles);
    }

    /**
     * 创建文档
     *
     * @param article 文章信息
     * @return 创建后的文档
     */
    @PostMapping("/articles")
    public Result<ArticleDocument> create(@RequestBody ArticleDocument article) {
        if (article.getCreateTime() == null) {
            article.setCreateTime(LocalDateTime.now());
        }
        ArticleDocument saved = articleRepository.save(article);
        log.info("[ES] 创建文档: id={}, title={}", saved.getId(), saved.getTitle());
        return Result.ok(saved);
    }

    /**
     * 删除文档
     *
     * @param id 文档 ID
     * @return 删除结果
     */
    @DeleteMapping("/articles/{id}")
    public Result<String> delete(@PathVariable String id) {
        articleRepository.deleteById(id);
        log.info("[ES] 删除文档: id={}", id);
        return Result.ok("文档删除成功: id=" + id);
    }
}
