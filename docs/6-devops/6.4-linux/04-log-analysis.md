---
title: "日志分析"
module: "linux"
difficulty: "intermediate"
interviewFrequency: "medium"
tags:
  - "Linux"
  - "日志分析"
  - "grep"
  - "awk"
  - "sed"
codeExample: ""
relatedEntries:
  - "/6-devops/6.4-linux/01-commands"
  - "/2-framework/2.2-springboot/10-logging"
prerequisites:
  - "/6-devops/6.4-linux/01-commands"
estimatedTime: "30min"
---

# 日志分析

## 概念说明

日志分析是线上问题排查的第一步。掌握 grep、awk、sed 三大文本处理工具的组合使用，能快速从海量日志中定位问题。

## 核心原理

### 一、grep — 文本搜索

```bash
# 基础搜索
grep "ERROR" app.log                    # 搜索包含 ERROR 的行
grep -i "error" app.log                 # 忽略大小写
grep -n "Exception" app.log             # 显示行号
grep -c "ERROR" app.log                 # 统计匹配行数
grep -v "DEBUG" app.log                 # 排除 DEBUG 行

# 上下文搜索
grep -A 5 "OutOfMemoryError" app.log    # 匹配行及后 5 行
grep -B 3 "ERROR" app.log              # 匹配行及前 3 行
grep -C 3 "NullPointerException" app.log # 匹配行及前后 3 行

# 正则搜索
grep -E "ERROR|WARN" app.log            # 搜索 ERROR 或 WARN
grep -P "\d{4}-\d{2}-\d{2}" app.log    # Perl 正则（日期格式）

# 递归搜索
grep -r "TODO" /opt/app/src/            # 递归搜索目录
grep -rl "password" /opt/app/config/    # 只显示文件名

# 实用组合
grep "ERROR" app.log | grep -v "HealthCheck"  # 排除健康检查的错误
grep "2024-01-15 14:" app.log | grep "ERROR"  # 搜索特定时间段的错误
```

### 二、awk — 文本处理

awk 是强大的文本处理工具，擅长按列处理结构化文本。

```bash
# 基础用法：按列提取
# 默认以空格/Tab 分隔
awk '{print $1}' access.log             # 提取第一列（IP）
awk '{print $1, $7}' access.log         # 提取 IP 和 URL
awk '{print NR, $0}' app.log            # 添加行号

# 指定分隔符
awk -F',' '{print $1, $3}' data.csv     # CSV 文件
awk -F':' '{print $1}' /etc/passwd      # 冒号分隔

# 条件过滤
awk '$9 == 500' access.log              # HTTP 500 错误
awk '$9 >= 400' access.log              # 所有 4xx/5xx 错误
awk '$10 > 1000000' access.log          # 响应体大于 1MB

# 统计计算
awk '{sum += $10} END {print "总流量:", sum/1024/1024, "MB"}' access.log
awk '{count[$1]++} END {for(ip in count) print count[ip], ip}' access.log | sort -rn | head -10
# 统计访问 IP Top 10

# 时间范围过滤
awk '$4 >= "[15/Jan/2024:14:00" && $4 <= "[15/Jan/2024:15:00"' access.log
```

### 三、sed — 流编辑器

sed 擅长文本替换和行操作。

```bash
# 文本替换
sed 's/old/new/' file.txt               # 替换每行第一个匹配
sed 's/old/new/g' file.txt              # 替换所有匹配
sed -i 's/old/new/g' file.txt           # 直接修改文件（-i）
sed -i.bak 's/old/new/g' file.txt       # 修改文件并备份

# 行操作
sed -n '10,20p' app.log                 # 打印第 10-20 行
sed -n '/ERROR/p' app.log               # 打印包含 ERROR 的行（类似 grep）
sed '/DEBUG/d' app.log                  # 删除 DEBUG 行
sed '1,5d' app.log                      # 删除前 5 行

# 实用场景
# 提取两个时间点之间的日志
sed -n '/2024-01-15 14:00/,/2024-01-15 15:00/p' app.log

# 替换配置文件中的端口
sed -i 's/server.port=8080/server.port=9090/' application.properties
```

### 四、常用日志分析命令组合

#### 场景 1：统计错误类型分布

```bash
grep "ERROR" app.log | awk -F'Exception' '{print $1}' | \
    awk '{print $NF}' | sort | uniq -c | sort -rn | head -10
```

#### 场景 2：统计每小时错误数

```bash
grep "ERROR" app.log | awk '{print substr($1,1,13)}' | \
    sort | uniq -c | sort -k2
# 输出示例：
#  15 2024-01-15 14
#  42 2024-01-15 15
#   8 2024-01-15 16
```

#### 场景 3：统计接口响应时间

```bash
# 假设日志格式：2024-01-15 14:30:00 [INFO] GET /api/users 200 150ms
grep "GET /api/users" app.log | awk '{print $NF}' | \
    sed 's/ms//' | awk '{sum+=$1; count++} END {print "平均:", sum/count, "ms"}'
```

#### 场景 4：查找慢 SQL

```bash
# 假设日志中有 SQL 执行时间
grep "SlowSQL" app.log | awk -F'time=' '{print $2}' | \
    awk -F'ms' '{print $1}' | sort -rn | head -10
```

#### 场景 5：统计 Nginx 访问 Top URL

```bash
awk '{print $7}' access.log | sort | uniq -c | sort -rn | head -20
```

#### 场景 6：统计 HTTP 状态码分布

```bash
awk '{print $9}' access.log | sort | uniq -c | sort -rn
# 输出示例：
# 85000 200
#  5000 304
#  2000 404
#   500 500
```

### 五、日志切割 — logrotate

```bash
# /etc/logrotate.d/myapp
/opt/app/logs/*.log {
    daily              # 每天切割
    rotate 30          # 保留 30 天
    compress           # 压缩旧日志
    delaycompress      # 延迟一天压缩
    missingok          # 日志不存在不报错
    notifempty         # 空文件不切割
    copytruncate       # 复制后截断（不需要重启应用）
    dateext            # 使用日期作为后缀
    dateformat -%Y%m%d # 日期格式
}

# 手动执行切割
logrotate -f /etc/logrotate.d/myapp

# 测试配置（不实际执行）
logrotate -d /etc/logrotate.d/myapp
```

## 常见面试题

### Q1: 如何从日志中统计访问量 Top 10 的 IP？

**难度**：⭐⭐ | **频率**：🔥🔥🔥

**标准答案**：

```bash
awk '{print $1}' access.log | sort | uniq -c | sort -rn | head -10
```

解释：`awk` 提取第一列（IP），`sort` 排序，`uniq -c` 去重并计数，`sort -rn` 按数量降序，`head -10` 取前 10。

### Q2: 如何查看某个时间段内的错误日志？

**难度**：⭐⭐ | **频率**：🔥🔥

**标准答案**：

```bash
# 方法一：grep 时间前缀
grep "2024-01-15 14:" app.log | grep "ERROR"

# 方法二：sed 范围提取
sed -n '/2024-01-15 14:00/,/2024-01-15 15:00/p' app.log | grep "ERROR"

# 方法三：awk 条件过滤
awk '/2024-01-15 14:00/,/2024-01-15 15:00/' app.log | grep "ERROR"
```

## 参考资料

- [GNU Grep Manual](https://www.gnu.org/software/grep/manual/grep.html)
- [The AWK Programming Language](https://awk.dev/)
- [GNU Sed Manual](https://www.gnu.org/software/sed/manual/sed.html)
