/**
 * RabbitMQ 消息队列知识代码示例
 *
 * <p>本模块包含 RabbitMQ 的核心概念演示、消息可靠性方案、高级特性和 Spring Boot 集成示例。</p>
 *
 * <h3>模块结构：</h3>
 * <ul>
 *   <li>core/ — Exchange 类型演示、消息发送接收模型说明</li>
 *   <li>reliability/ — 消息确认、持久化、幂等性方案</li>
 *   <li>advanced/ — 死信队列、延迟消息实现</li>
 *   <li>spring/ — RabbitTemplate、@RabbitListener 使用</li>
 * </ul>
 *
 * <p>⚠️ 本模块为概念说明型代码，通过注释和打印输出说明 RabbitMQ 的核心概念和使用方式。
 * 实际连接 RabbitMQ 需要启动 Docker 环境：</p>
 * <pre>docker compose -f docker/docker-compose.mq.yml up -d</pre>
 *
 * @see <a href="https://www.rabbitmq.com/docs">RabbitMQ 官方文档</a>
 */
package com.example.mq.rabbitmq;
