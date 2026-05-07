---
title: "Docker/K8s 常用命令速查表"
module: "docker-k8s"
difficulty: "beginner"
interviewFrequency: "medium"
tags:
  - "Docker"
  - "Kubernetes"
  - "命令速查"
codeExample: ""
relatedEntries:
  - "/6-devops/6.1-docker-k8s/01-docker-basics"
  - "/6-devops/6.1-docker-k8s/06-k8s-architecture"
prerequisites: []
estimatedTime: "15min"
---

# Docker/K8s 常用命令速查表

## Docker 命令

### 镜像管理

```bash
docker pull <image>:<tag>          # 拉取镜像
docker images                       # 列出本地镜像
docker rmi <image>                  # 删除镜像
docker build -t <name>:<tag> .      # 构建镜像
docker tag <image> <new-name>:<tag> # 给镜像打标签
docker push <image>:<tag>           # 推送镜像到仓库
docker image prune                  # 清理悬空镜像
docker system prune -a              # 清理所有未使用资源
```

### 容器管理

```bash
docker run -d --name <name> -p 8080:8080 <image>  # 后台运行容器
docker ps                           # 查看运行中的容器
docker ps -a                        # 查看所有容器
docker stop <container>             # 停止容器
docker start <container>            # 启动已停止的容器
docker restart <container>          # 重启容器
docker rm <container>               # 删除容器
docker rm -f $(docker ps -aq)       # 删除所有容器
```

### 容器调试

```bash
docker logs -f <container>          # 实时查看日志
docker logs --tail 100 <container>  # 查看最后 100 行日志
docker exec -it <container> /bin/sh # 进入容器
docker inspect <container>          # 查看容器详情
docker stats                        # 查看容器资源使用
docker top <container>              # 查看容器进程
docker cp <container>:/path /local  # 从容器复制文件
```

### Docker Compose

```bash
docker compose up -d                # 启动所有服务
docker compose down                 # 停止并删除所有服务
docker compose down -v              # 停止并删除服务+数据卷
docker compose ps                   # 查看服务状态
docker compose logs -f <service>    # 查看服务日志
docker compose exec <service> sh    # 进入服务容器
docker compose up -d --build        # 重新构建并启动
docker compose pull                 # 拉取最新镜像
```

## Kubernetes 命令

### 集群信息

```bash
kubectl cluster-info                # 集群信息
kubectl get nodes -o wide           # 节点列表
kubectl top nodes                   # 节点资源使用
kubectl get namespaces              # 命名空间列表
```

### Pod 管理

```bash
kubectl get pods                    # 查看 Pod 列表
kubectl get pods -o wide            # 查看 Pod 详情（含 IP 和节点）
kubectl describe pod <pod>          # 查看 Pod 详细信息
kubectl logs <pod>                  # 查看 Pod 日志
kubectl logs -f <pod> -c <container> # 实时查看指定容器日志
kubectl exec -it <pod> -- /bin/sh   # 进入 Pod
kubectl delete pod <pod>            # 删除 Pod
kubectl top pods                    # 查看 Pod 资源使用
```

### Deployment 管理

```bash
kubectl get deployments             # 查看 Deployment 列表
kubectl apply -f deployment.yaml    # 创建/更新 Deployment
kubectl rollout status deploy/<name> # 查看滚动更新状态
kubectl rollout history deploy/<name> # 查看更新历史
kubectl rollout undo deploy/<name>  # 回滚到上一版本
kubectl scale deploy/<name> --replicas=5 # 手动扩缩容
kubectl set image deploy/<name> <container>=<image>:<tag> # 更新镜像
```

### Service 与 Ingress

```bash
kubectl get svc                     # 查看 Service 列表
kubectl get ingress                 # 查看 Ingress 列表
kubectl describe svc <name>         # 查看 Service 详情
kubectl port-forward svc/<name> 8080:80 # 端口转发（本地调试）
```

### ConfigMap 与 Secret

```bash
kubectl get configmaps              # 查看 ConfigMap 列表
kubectl get secrets                 # 查看 Secret 列表
kubectl create configmap <name> --from-file=config.yml # 从文件创建
kubectl create secret generic <name> --from-literal=key=value # 创建 Secret
```

### 调试与排查

```bash
kubectl describe pod <pod>          # 查看事件和状态
kubectl logs <pod> --previous       # 查看上一次容器的日志
kubectl get events --sort-by='.lastTimestamp' # 查看集群事件
kubectl run debug --image=busybox -it --rm -- sh # 临时调试 Pod
kubectl port-forward <pod> 8080:8080 # 端口转发
```

### Helm 命令

```bash
helm repo add <name> <url>          # 添加仓库
helm repo update                    # 更新仓库
helm search repo <keyword>          # 搜索 Chart
helm install <release> <chart>      # 安装 Chart
helm upgrade <release> <chart>      # 升级 Release
helm rollback <release> <revision>  # 回滚
helm uninstall <release>            # 卸载
helm list                           # 查看已安装的 Release
helm template <release> <chart>     # 渲染模板（调试）
```

## 参考资料

- [Docker CLI 参考](https://docs.docker.com/reference/cli/docker/)
- [kubectl 参考](https://kubernetes.io/zh-cn/docs/reference/kubectl/)
- [Helm 命令参考](https://helm.sh/zh/docs/helm/)
