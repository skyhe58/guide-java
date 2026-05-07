/**
 * Kafka 消息队列知识代码示例
 *
 * <p>本模块包含 Kafka 的核心概念演示、消息可靠性方案、高级特性和 Spring Boot 集成示例。</p>
 *
 * <h3>模块结构：</h3>
 * <ul>
 *   <li>core/ — Kafka 架构概念、Producer/Consumer 模型说明</li>
 *   <li>reliability/ — acks 配置、幂等性、事务消息</li>
 *   <li>advanced/ — 分区策略、Rebalance、Offset 管理</li>
 *   <li>spring/ — KafkaTemplate、@KafkaListener 使用</li>
 * </ul>
 *
 * <p>⚠️ 本模块为概念说明型代码，通过注释和打印输出说明 Kafka 的核心概念和使用方式。
 * 实际连接 Kafka 需要启动 Docker 环境：</p>
 * <pre>docker compose -f docker/docker-compose.mq.yml up -d</pre>
 *
 * @see <a href="https://kafka.apache.org/documentation/">Apache Kafka Documentation</a>
 */
package com.example.mq.kafka;
