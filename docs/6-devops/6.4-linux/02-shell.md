---
title: "Shell 脚本基础"
module: "linux"
difficulty: "intermediate"
interviewFrequency: "medium"
tags:
  - "Linux"
  - "Shell"
  - "Bash"
  - "脚本"
codeExample: ""
relatedEntries:
  - "/6-devops/6.4-linux/01-commands"
  - "/6-devops/6.4-linux/04-log-analysis"
prerequisites:
  - "/6-devops/6.4-linux/01-commands"
estimatedTime: "45min"
---

# Shell 脚本基础

## 概念说明

Shell 脚本是 Linux 系统管理和自动化运维的核心工具。对于 Java 后端开发者，掌握 Shell 脚本能帮助你编写部署脚本、日志分析脚本和监控脚本，提升运维效率。

## 核心原理

### 一、基础语法

#### 脚本结构

```bash
#!/bin/bash
# 第一行 shebang 指定解释器

# 变量定义（等号两边不能有空格）
APP_NAME="my-app"
APP_PORT=8080
APP_HOME="/opt/${APP_NAME}"

# 使用变量
echo "应用名称: ${APP_NAME}"
echo "应用端口: ${APP_PORT}"

# 只读变量
readonly VERSION="1.0.0"

# 命令替换
CURRENT_DATE=$(date '+%Y-%m-%d %H:%M:%S')
JAVA_PID=$(pgrep -f "${APP_NAME}")
echo "当前时间: ${CURRENT_DATE}"
```

#### 特殊变量

```bash
#!/bin/bash
# $0 - 脚本名称
# $1, $2, ... - 位置参数
# $# - 参数个数
# $@ - 所有参数（作为独立字符串）
# $* - 所有参数（作为单个字符串）
# $? - 上一个命令的退出状态（0=成功）
# $$ - 当前脚本的 PID

echo "脚本名: $0"
echo "第一个参数: $1"
echo "参数个数: $#"
echo "所有参数: $@"
```

### 二、条件判断

```bash
#!/bin/bash

# if-elif-else
if [ -f "/opt/app/app.jar" ]; then
    echo "应用文件存在"
elif [ -d "/opt/app/" ]; then
    echo "目录存在但 jar 文件不存在"
else
    echo "目录不存在"
fi

# 文件测试
# -f 文件存在且是普通文件
# -d 目录存在
# -e 文件/目录存在
# -r 可读  -w 可写  -x 可执行
# -s 文件存在且非空

# 字符串比较
if [ "$APP_ENV" = "production" ]; then
    echo "生产环境"
fi

# 数值比较
# -eq 等于  -ne 不等于  -gt 大于  -lt 小于  -ge 大于等于  -le 小于等于
if [ $APP_PORT -gt 0 ] && [ $APP_PORT -lt 65536 ]; then
    echo "端口号有效"
fi

# 使用 [[ ]] 支持更多特性（推荐）
if [[ "$APP_NAME" == my-* ]]; then
    echo "应用名以 my- 开头"
fi
```

### 三、循环

```bash
#!/bin/bash

# for 循环
for server in web01 web02 web03; do
    echo "部署到 ${server}..."
done

# C 风格 for 循环
for ((i=1; i<=5; i++)); do
    echo "第 ${i} 次重试..."
done

# while 循环
count=0
while [ $count -lt 5 ]; do
    echo "等待服务启动... (${count}/5)"
    sleep 2
    count=$((count + 1))
done

# 读取文件逐行处理
while IFS= read -r line; do
    echo "处理: ${line}"
done < servers.txt
```

### 四、函数

```bash
#!/bin/bash

# 函数定义
check_port() {
    local port=$1
    if ss -tlnp | grep -q ":${port} "; then
        echo "端口 ${port} 已被占用"
        return 1
    else
        echo "端口 ${port} 可用"
        return 0
    fi
}

# 函数调用
check_port 8080
if [ $? -eq 0 ]; then
    echo "可以启动应用"
fi

# 带返回值的函数
get_java_pid() {
    local app_name=$1
    local pid=$(pgrep -f "${app_name}")
    echo "${pid}"  # 通过 echo 返回值
}

# 获取返回值
PID=$(get_java_pid "my-app")
echo "Java PID: ${PID}"
```

### 五、常用脚本模板

#### 模板 1：Java 应用启停脚本

```bash
#!/bin/bash
# Java 应用启停脚本

APP_NAME="my-app"
APP_JAR="${APP_NAME}.jar"
APP_HOME="/opt/${APP_NAME}"
LOG_FILE="${APP_HOME}/logs/app.log"
JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"

start() {
    local pid=$(pgrep -f "${APP_JAR}")
    if [ -n "${pid}" ]; then
        echo "${APP_NAME} 已在运行 (PID: ${pid})"
        return 1
    fi

    echo "启动 ${APP_NAME}..."
    nohup java ${JAVA_OPTS} -jar ${APP_HOME}/${APP_JAR} \
        --spring.profiles.active=prod \
        > ${LOG_FILE} 2>&1 &

    sleep 3
    pid=$(pgrep -f "${APP_JAR}")
    if [ -n "${pid}" ]; then
        echo "${APP_NAME} 启动成功 (PID: ${pid})"
    else
        echo "${APP_NAME} 启动失败，请查看日志: ${LOG_FILE}"
        return 1
    fi
}

stop() {
    local pid=$(pgrep -f "${APP_JAR}")
    if [ -z "${pid}" ]; then
        echo "${APP_NAME} 未在运行"
        return 0
    fi

    echo "停止 ${APP_NAME} (PID: ${pid})..."
    kill ${pid}

    # 等待进程退出（最多 30 秒）
    for i in $(seq 1 30); do
        if ! kill -0 ${pid} 2>/dev/null; then
            echo "${APP_NAME} 已停止"
            return 0
        fi
        sleep 1
    done

    echo "强制杀死 ${APP_NAME}..."
    kill -9 ${pid}
}

status() {
    local pid=$(pgrep -f "${APP_JAR}")
    if [ -n "${pid}" ]; then
        echo "${APP_NAME} 运行中 (PID: ${pid})"
    else
        echo "${APP_NAME} 未运行"
    fi
}

case "$1" in
    start)  start ;;
    stop)   stop ;;
    restart) stop; sleep 2; start ;;
    status) status ;;
    *)
        echo "用法: $0 {start|stop|restart|status}"
        exit 1
        ;;
esac
```

#### 模板 2：日志清理脚本

```bash
#!/bin/bash
# 清理 N 天前的日志文件

LOG_DIR="/opt/app/logs"
KEEP_DAYS=7

echo "清理 ${KEEP_DAYS} 天前的日志..."
find ${LOG_DIR} -name "*.log" -mtime +${KEEP_DAYS} -exec rm -f {} \;
find ${LOG_DIR} -name "*.log.gz" -mtime +${KEEP_DAYS} -exec rm -f {} \;

echo "清理完成，当前日志文件："
ls -lh ${LOG_DIR}/*.log 2>/dev/null
echo "磁盘使用: $(du -sh ${LOG_DIR})"
```

#### 模板 3：健康检查脚本

```bash
#!/bin/bash
# 应用健康检查脚本

APP_URL="http://localhost:8080/actuator/health"
MAX_RETRY=3
RETRY_INTERVAL=5

check_health() {
    local status_code=$(curl -o /dev/null -s -w "%{http_code}" ${APP_URL})
    if [ "${status_code}" = "200" ]; then
        return 0
    else
        return 1
    fi
}

for i in $(seq 1 ${MAX_RETRY}); do
    if check_health; then
        echo "✅ 应用健康 (第 ${i} 次检查)"
        exit 0
    else
        echo "⚠️ 应用不健康 (第 ${i}/${MAX_RETRY} 次检查)"
        if [ $i -lt ${MAX_RETRY} ]; then
            echo "等待 ${RETRY_INTERVAL} 秒后重试..."
            sleep ${RETRY_INTERVAL}
        fi
    fi
done

echo "❌ 应用健康检查失败！"
# 可以在这里添加告警逻辑
exit 1
```

## 常见面试题

### Q1: Shell 脚本中 `$?`、`$#`、`$@` 分别是什么？

**难度**：⭐⭐ | **频率**：🔥🔥

**标准答案**：

`$?` 是上一个命令的退出状态码（0 表示成功，非 0 表示失败）；`$#` 是传入脚本的参数个数；`$@` 是所有参数列表，每个参数作为独立字符串。`$*` 也是所有参数，但作为一个整体字符串。`$$` 是当前脚本的 PID。

### Q2: `[` 和 `[[` 的区别？

**难度**：⭐⭐ | **频率**：🔥

**标准答案**：

`[` 是 `test` 命令的别名，是 POSIX 标准，兼容性好但功能有限。`[[` 是 Bash 内置关键字，支持正则匹配（`=~`）、模式匹配（`==` 配合通配符）、逻辑运算符（`&&`、`||`），且不需要对变量加引号防止分词。推荐在 Bash 脚本中使用 `[[`。

## 参考资料

- [Bash Reference Manual](https://www.gnu.org/software/bash/manual/bash.html)
- [Shell 脚本编程规范](https://google.github.io/styleguide/shellguide.html)
