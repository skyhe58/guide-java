---
title: "监控体系模块概述"
module: "monitoring"
difficulty: "intermediate"
interviewFrequency: "medium"
tags:
  - "监控"
  - "Prometheus"
  - "Grafana"
  - "模块概述"
codeExample: "06-devops/monitoring-examples/"
relatedEntries:
  - "/6-devops/6.1-docker-k8s/00-index"
  - "/6-devops/6.2-cicd/00-index"
prerequisites:
  - "/2-framework/2.2-springboot/13-actuator"
estimatedTime: "15min"
---

# 监控体系模块概述

## 概念说明

完善的监控体系是保障系统稳定运行的基石。Java 后端监控通常包括**指标监控**（Prometheus + Grafana）、**日志监控**（ELK/Loki）和**链路追踪**（SkyWalking/Zipkin）三大支柱。

## 模块知识图谱

```mermaid
graph TD
    A["监控体系"] --> B["指标监控"]
    A --> C["可视化"]
    A --> D["应用集成"]
    A --> E["日志监控"]

    B --> B1["Prometheus 指标采集"]
    B --> B2["PromQL 查询语言"]
    B --> B3["告警规则 AlertManager"]

    C --> C1["Grafana 仪表盘"]
    C --> C2["告警通知"]

    D --> D1["Micrometer 指标门面"]
    D --> D2["Spring Boot Actuator"]
    D --> D3["自定义业务指标"]

    E --> E1["ELK Stack"]
    E --> E2["Loki + Grafana"]
    E --> E3["结构化日志"]

    style A fill:#e1f5fe
    style B fill:#e8f5e9
    style C fill:#fff3e0
    style D fill:#fce4ec
    style E fill:#f3e5f5
```

## 推荐学习顺序

| 序号 | 知识点 | 文档 | 建议时间 |
|------|--------|------|----------|
| 1 | Prometheus 指标采集 | [01-prometheus](./01-prometheus.md) | 40min |
| 2 | Grafana 仪表盘 | [02-grafana](./02-grafana.md) | 35min |
| 3 | Micrometer 集成 | [03-micrometer](./03-micrometer.md) | 40min |
| 4 | 日志监控方案 | [04-log-monitoring](./04-log-monitoring.md) | 35min |
| 5 | 面试指南 | [99-interview](./99-interview.md) | 20min |

## 监控三大支柱

| 支柱 | 工具 | 关注点 |
|------|------|--------|
| 指标（Metrics） | Prometheus + Grafana | CPU/内存/QPS/延迟/错误率 |
| 日志（Logging） | ELK / Loki | 错误日志/业务日志/审计日志 |
| 链路追踪（Tracing） | SkyWalking / Zipkin | 请求链路/耗时分析/瓶颈定位 |

## 代码示例

> 💻 完整可运行代码：[code-examples/06-devops/monitoring-examples/](../../../code-examples/06-devops/monitoring-examples/)

## 相关模块

- [Spring Boot Actuator](../../2-framework/2.2-springboot/13-actuator.md) — 应用健康检查与指标暴露
- [Docker 与 K8s](../6.1-docker-k8s/00-index.md) — 容器监控
- [CI/CD](../6.2-cicd/00-index.md) — 部署后的监控
