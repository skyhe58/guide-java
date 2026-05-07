/**
 * Elasticsearch 知识代码示例
 *
 * <p>本模块包含 Elasticsearch 的核心知识代码示例：</p>
 * <ul>
 *   <li>{@link com.example.es.index_demo} — 倒排索引原理说明、分词器对比</li>
 *   <li>{@link com.example.es.crud} — 文档 CRUD、批量操作、乐观锁</li>
 *   <li>{@link com.example.es.query} — DSL 查询构建、分页方案对比</li>
 *   <li>{@link com.example.es.aggregation} — 聚合分析（Bucket/Metric/Pipeline）</li>
 *   <li>{@link com.example.es.spring} — Spring Data Elasticsearch 集成配置</li>
 * </ul>
 *
 * <p>⚠️ 需要 ES 环境：{@code docker compose -f docker/docker-compose.es.yml up -d}</p>
 *
 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html">Elasticsearch Reference</a>
 */
package com.example.es;
