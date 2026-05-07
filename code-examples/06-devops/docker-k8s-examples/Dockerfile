# ============================================================
# Java 应用标准 Dockerfile
# 适用于 Spring Boot 应用的生产级 Dockerfile
# ============================================================

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 安全：创建非 root 用户
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# 复制应用 JAR（假设已通过 CI 构建好）
COPY target/*.jar app.jar

# 切换到非 root 用户
USER appuser

# JVM 容器优化参数
# -XX:MaxRAMPercentage=75.0  堆内存占容器内存的 75%，预留 25% 给非堆和 OS
# -XX:InitialRAMPercentage=50.0  初始堆内存占 50%
# -XX:+UseG1GC  使用 G1 垃圾收集器
# -XX:+UseContainerSupport  启用容器感知（JDK 8u191+ 默认开启）
# -Djava.security.egd  加速随机数生成（避免容器中 /dev/random 阻塞）
ENV JAVA_OPTS="\
  -XX:MaxRAMPercentage=75.0 \
  -XX:InitialRAMPercentage=50.0 \
  -XX:+UseG1GC \
  -XX:+UseContainerSupport \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/tmp/heapdump.hprof \
  -Djava.security.egd=file:/dev/./urandom"

# 暴露端口
EXPOSE 8080

# 支持优雅停机（Spring Boot 配合 server.shutdown=graceful）
STOPSIGNAL SIGTERM

# 启动命令（通过 sh -c 支持环境变量展开）
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
