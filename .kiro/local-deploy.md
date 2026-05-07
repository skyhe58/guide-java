# 本地部署流程（不提交到 Git）

## 一、VitePress 文档站点

### 环境要求
- Node.js 18+
- pnpm 8+

### 启动本地预览

```bash
cd docs
pnpm install        # 首次安装依赖
pnpm run dev        # 启动开发服务器
```

浏览器打开 `http://localhost:5173`

### 构建静态文件

```bash
cd docs
pnpm run build      # 构建到 docs/.vitepress/dist/
pnpm run preview    # 预览构建结果 http://localhost:4173
```

### 常见问题

**Q: 页面空白，控制台报 dayjs ESM 错误**
A: `package.json` 中已通过 `pnpm.overrides` 锁定 `dayjs@1.11.13`，执行：
```bash
cd docs
rm -rf node_modules pnpm-lock.yaml .vitepress/cache
pnpm install
pnpm run dev
```

**Q: 点击代码链接（如 RegistryController.java）404**
A: 正常现象。代码链接指向 `code-examples/` 下的 Java 文件，不在 `docs/` 目录内，VitePress 无法渲染。
- 本地：直接在 IDE 中打开对应文件
- GitHub Pages：部署后链接会指向 GitHub 仓库源码页面

## 二、Java 代码编译

### 环境要求
- JDK 21
- Maven 3.8+

### 编译全部模块

```bash
cd code-examples
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home mvn compile
```

### 编译单个模块

```bash
cd code-examples
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home mvn compile -pl 02-framework/springcloud-examples -q
```

## 三、Spring Cloud 实战项目

### 启动中间件

```bash
docker compose -f docker/docker-compose.yml up -d          # MySQL + Redis + MongoDB + MinIO
docker compose -f docker/docker-compose.consul.yml up -d    # Consul
docker compose -f docker/docker-compose.mq.yml up -d        # RabbitMQ + Kafka + ZooKeeper
docker compose -f docker/docker-compose.es.yml up -d        # Elasticsearch
```

### 启动微服务

```bash
# 业务服务（端口 8090）
cd code-examples/02-framework/springcloud-examples
mvn spring-boot:run

# 网关（端口 8080）
cd code-examples/02-framework/springcloud-gateway
mvn spring-boot:run

# 分库分表（端口 8091）
cd code-examples/02-framework/springcloud-sharding
mvn spring-boot:run

# 用户服务（端口 8092）
cd code-examples/02-framework/springcloud-user-service
mvn spring-boot:run
```

### 验证

```bash
curl http://localhost:8090/demo/registry/services
curl http://localhost:8090/demo/cache/set?key=test&value=hello
curl http://localhost:8090/actuator/health
```

### Profile 切换

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=nacos    # Nacos 注册中心
mvn spring-boot:run -Dspring-boot.run.profiles=zk       # ZooKeeper 注册中心
mvn spring-boot:run -Dspring-boot.run.profiles=sentinel  # Sentinel 熔断
```
