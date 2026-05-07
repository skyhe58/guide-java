---
title: "Docker/K8s 面试指南"
module: "docker-k8s"
difficulty: "intermediate"
interviewFrequency: "high"
tags:
  - "Docker"
  - "Kubernetes"
  - "面试"
  - "面试高频"
relatedEntries:
  - "/6-devops/6.1-docker-k8s/01-docker-basics"
  - "/6-devops/6.1-docker-k8s/06-k8s-architecture"
  - "/6-devops/6.1-docker-k8s/05-java-docker"
estimatedTime: "30min"
---

# Docker/K8s 面试指南

## 面试知识图谱

```mermaid
graph TD
    A["Docker/K8s 面试知识图谱"] --> B["Docker 基础"]
    A --> C["Dockerfile"]
    A --> D["K8s 架构"]
    A --> E["K8s 资源"]
    A --> F["部署运维"]

    B --> B1["镜像/容器/仓库"]
    B --> B2["Docker vs 虚拟机"]
    B --> B3["Namespace/Cgroup"]
    B --> B4["网络模式"]

    C --> C1["多阶段构建"]
    C --> C2["镜像瘦身"]
    C --> C3["缓存优化"]
    C --> C4["Java 容器调优"]

    D --> D1["Master 组件"]
    D --> D2["Node 组件"]
    D --> D3["etcd"]
    D --> D4["Pod 创建流程"]

    E --> E1["Pod/Deployment"]
    E --> E2["Service 类型"]
    E --> E3["ConfigMap/Secret"]
    E --> E4["Ingress"]

    F --> F1["三种探针"]
    F --> F2["部署策略"]
    F --> F3["HPA 扩缩容"]
    F --> F4["Helm"]
```

## 高频面试题汇总

### 🔥🔥🔥 必问题

#### Q1: Docker 和虚拟机有什么区别？

详见 [Docker 核心概念](./01-docker-basics.md)

**核心要点**：Docker 容器直接运行在宿主机内核上（Namespace 隔离 + Cgroup 限制），启动秒级、MB 级资源占用；虚拟机通过 Hypervisor 模拟硬件，运行独立 Guest OS，启动分钟级、GB 级资源占用。

#### Q2: 如何优化 Docker 镜像大小？

详见 [Dockerfile 最佳实践](./02-dockerfile.md)

**核心要点**：多阶段构建、Alpine 基础镜像、只安装 JRE、合并 RUN 指令、.dockerignore。

#### Q3: Java 应用在 Docker 中运行需要注意什么？

详见 [Java 应用 Docker 化](./05-java-docker.md)

**核心要点**：JVM 容器感知（UseContainerSupport）、MaxRAMPercentage=75.0、非 root 用户、优雅停机。

#### Q4: K8s 的架构和核心组件？

详见 [K8s 架构](./06-k8s-architecture.md)

**核心要点**：Master（API Server + etcd + Scheduler + Controller Manager）+ Node（kubelet + kube-proxy + 容器运行时）。

#### Q5: K8s 的三种探针有什么区别？

详见 [K8s 健康检查](./08-k8s-health.md)

**核心要点**：startupProbe（启动检测）、livenessProbe（存活检测，失败重启）、readinessProbe（就绪检测，失败摘除流量）。

#### Q6: K8s 有哪些部署策略？

详见 [K8s 部署策略](./09-k8s-deploy.md)

**核心要点**：滚动更新（默认，maxSurge/maxUnavailable）、蓝绿部署（双环境切换）、金丝雀发布（灰度验证）。

### 🔥🔥 常问题

#### Q7: Pod 和容器的关系？

详见 [K8s 核心资源对象](./07-k8s-resources.md)

**核心要点**：Pod 是最小调度单元，可包含多个容器，共享网络和存储。

#### Q8: Service 的几种类型？

详见 [K8s 核心资源对象](./07-k8s-resources.md)

**核心要点**：ClusterIP（集群内部）、NodePort（节点端口）、LoadBalancer（云 LB）。

#### Q9: HPA 是如何工作的？

详见 [HPA 自动扩缩容](./10-k8s-hpa.md)

**核心要点**：定期查询 Metrics Server，根据指标计算期望副本数，自动调整 Deployment replicas。

#### Q10: Helm 是什么？解决了什么问题？

详见 [Helm 包管理](./11-helm.md)

**核心要点**：K8s 包管理工具，Chart 打包 + 模板参数化 + 版本管理 + 一键回滚。

### 🔥 偶尔问

#### Q11: Docker 网络模式有哪些？

详见 [Docker 网络模式](./03-docker-network.md)

#### Q12: Docker Compose 中 depends_on 能保证服务就绪吗？

详见 [Docker Compose 编排](./04-docker-compose.md)

## 面试追问链路

```mermaid
graph LR
    A["Docker 基础"] -->|追问| B["镜像分层原理"]
    B -->|追问| C["Union FS"]
    A -->|追问| D["Namespace/Cgroup"]

    E["Java Docker 化"] -->|追问| F["JVM 内存感知"]
    F -->|追问| G["MaxRAMPercentage"]
    G -->|追问| H["OOM Kill 排查"]

    I["K8s 架构"] -->|追问| J["Pod 创建流程"]
    J -->|追问| K["Scheduler 调度策略"]

    L["部署策略"] -->|追问| M["滚动更新参数"]
    M -->|追问| N["优雅停机"]
    N -->|追问| O["readinessProbe"]
```

## 参考资料

- [Docker 官方文档](https://docs.docker.com/)
- [Kubernetes 官方文档](https://kubernetes.io/zh-cn/docs/)
- [Helm 官方文档](https://helm.sh/zh/docs/)
