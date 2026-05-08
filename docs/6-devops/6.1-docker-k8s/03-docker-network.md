---
title: "Docker 网络模式"
module: "docker-k8s"
difficulty: "intermediate"
interviewFrequency: "medium"
tags:
  - "Docker"
  - "网络"
  - "bridge"
  - "overlay"
codeExample: "06-devops/docker-k8s-examples/docker-compose.yml"
relatedEntries:
  - "/6-devops/6.1-docker-k8s/01-docker-basics"
  - "/6-devops/6.1-docker-k8s/04-docker-compose"
prerequisites:
  - "/6-devops/6.1-docker-k8s/01-docker-basics"
estimatedTime: "30min"
---

# Docker 网络模式

## 概念说明

Docker 提供多种网络驱动来满足不同场景的容器通信需求。理解网络模式是排查容器间通信问题和设计微服务部署方案的基础。

## 核心原理

### 五种网络模式对比

```mermaid
graph TB
    subgraph "bridge 模式（默认）"
        B_HOST["宿主机 eth0"]
        B_BRIDGE["docker0 网桥"]
        B_C1["容器1 veth"]
        B_C2["容器2 veth"]
        B_HOST --- B_BRIDGE
        B_BRIDGE --- B_C1
        B_BRIDGE --- B_C2
    end

    subgraph "host 模式"
        H_HOST["宿主机网络栈"]
        H_C1["容器1（共享宿主机网络）"]
        H_HOST --- H_C1
    end

    subgraph "overlay 模式"
        O_H1["宿主机1"]
        O_H2["宿主机2"]
        O_VXLAN["VXLAN 隧道"]
        O_C1["容器1"]
        O_C2["容器2"]
        O_H1 --- O_VXLAN --- O_H2
        O_H1 --- O_C1
        O_H2 --- O_C2
    end
```

| 网络模式 | 隔离性 | 性能 | 适用场景 |
|----------|--------|------|----------|
| bridge | 高 | 有 NAT 开销 | 单机多容器（默认模式） |
| host | 无 | 最高（无网络虚拟化） | 对网络性能要求极高的场景 |
| none | 完全隔离 | — | 安全敏感、自定义网络 |
| overlay | 跨主机隔离 | VXLAN 封装开销 | Docker Swarm / 跨主机通信 |
| macvlan | 高 | 接近物理网络 | 需要容器拥有独立 MAC 地址 |

### bridge 模式详解

bridge 是 Docker 默认网络模式，通过 `docker0` 虚拟网桥连接容器：

```mermaid
sequenceDiagram
    participant C1 as 容器1 (172.17.0.2)
    participant Bridge as docker0 网桥 (172.17.0.1)
    participant Host as 宿主机 (eth0)
    participant C2 as 容器2 (172.17.0.3)

    C1->>Bridge: 发送数据包
    Bridge->>C2: 同网桥直接转发
    C1->>Bridge: 访问外网
    Bridge->>Host: NAT 转换（iptables）
    Host->>Bridge: 端口映射 -p 8080:8080
    Bridge->>C1: 转发到容器
```

### 自定义网络

```bash
# 创建自定义 bridge 网络
docker network create --driver bridge my-network

# 容器加入自定义网络（支持 DNS 服务发现）
docker run -d --name app --network my-network my-app
docker run -d --name db --network my-network mysql

# 在自定义网络中，容器可通过名称互相访问
# app 容器内可直接用 db:3306 连接数据库
```

> 自定义 bridge 网络比默认 bridge 多了 DNS 服务发现功能，推荐使用。

## 代码示例

```yaml
# docker-compose.yml 中的网络配置
services:
  app:
    image: my-java-app
    networks:
      - backend
  redis:
    image: redis:7-alpine
    networks:
      - backend

networks:
  backend:
    driver: bridge
```

> 💻 完整编排示例：[code-examples/06-devops/docker-k8s-examples/docker-compose.yml](https://github.com/skyhe58/guide-java/tree/main/code-examples/06-devops/docker-k8s-examples/docker-compose.yml)
> <!-- 本地路径：code-examples/06-devops/docker-k8s-examples/docker-compose.yml -->

## 常见面试题

### Q1: Docker 有哪些网络模式？分别适用什么场景？

**难度**：⭐⭐⭐ | **频率**：🔥🔥

**标准答案**：

Docker 有五种网络模式：①bridge（默认）：通过虚拟网桥连接容器，适合单机多容器通信；②host：容器直接使用宿主机网络栈，性能最高但无隔离；③none：无网络，适合安全敏感场景；④overlay：通过 VXLAN 实现跨主机容器通信，用于 Swarm/K8s；⑤macvlan：为容器分配独立 MAC 地址，适合需要直接接入物理网络的场景。

**深入追问**：

- bridge 模式下容器如何访问外网？（NAT + iptables）
- 自定义 bridge 网络和默认 bridge 有什么区别？（DNS 服务发现）

## 参考资料

- [Docker 网络概述](https://docs.docker.com/network/)
