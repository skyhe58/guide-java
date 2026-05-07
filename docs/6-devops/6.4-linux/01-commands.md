---
title: "Linux 常用命令速查"
module: "linux"
difficulty: "beginner"
interviewFrequency: "high"
tags:
  - "Linux"
  - "命令"
  - "文件操作"
  - "进程管理"
  - "网络诊断"
  - "面试高频"
codeExample: ""
relatedEntries:
  - "/6-devops/6.4-linux/02-shell"
  - "/6-devops/6.4-linux/03-performance"
prerequisites: []
estimatedTime: "45min"
---

# Linux 常用命令速查

## 概念说明

本文按使用场景分类整理 Java 后端开发者最常用的 Linux 命令，覆盖文件操作、进程管理、网络诊断、磁盘管理和用户权限五大类。

## 核心命令

### 一、文件操作

#### 基础文件命令

```bash
# 列出文件（详细信息 + 隐藏文件）
ls -la

# 查看文件内容
cat file.txt              # 全部输出
head -n 20 file.txt       # 前 20 行
tail -n 50 file.txt       # 后 50 行
tail -f app.log           # 实时追踪日志（最常用）
tail -f app.log | grep ERROR  # 实时追踪错误日志

# 复制/移动/删除
cp -r src/ dest/           # 递归复制目录
mv old.txt new.txt         # 重命名/移动
rm -rf dir/                # 强制递归删除（慎用！）

# 创建目录
mkdir -p /opt/app/logs     # 递归创建多级目录
```

#### 文件查找

```bash
# find：按条件查找文件
find /opt/app -name "*.log"                    # 按名称查找
find /opt/app -name "*.log" -mtime +7          # 7 天前修改的日志
find /opt/app -name "*.log" -size +100M        # 大于 100M 的日志
find /opt/app -name "*.java" -exec grep -l "TODO" {} \;  # 查找含 TODO 的 Java 文件

# which / whereis：查找命令位置
which java                 # Java 可执行文件路径
whereis java               # Java 相关文件路径

# locate：快速查找（需要 updatedb）
locate application.yml
```

#### 文件权限

```bash
# 查看权限
ls -la file.txt
# -rw-r--r-- 1 user group 1024 Jan 1 00:00 file.txt
# 权限说明：rw-(所有者) r--(组) r--(其他)

# 修改权限
chmod 755 script.sh        # rwxr-xr-x（所有者可执行）
chmod +x script.sh         # 添加执行权限
chmod -R 644 /opt/app/     # 递归修改

# 修改所有者
chown user:group file.txt
chown -R appuser:appgroup /opt/app/

# 常用权限数字
# 7=rwx  6=rw-  5=r-x  4=r--  0=---
```

#### 压缩与解压

```bash
# tar
tar -czf app.tar.gz /opt/app/     # 压缩
tar -xzf app.tar.gz               # 解压
tar -xzf app.tar.gz -C /opt/      # 解压到指定目录

# zip
zip -r app.zip /opt/app/          # 压缩
unzip app.zip -d /opt/            # 解压
```

### 二、进程管理

```bash
# 查看进程
ps -ef | grep java                # 查找 Java 进程
ps aux --sort=-%mem | head -10    # 按内存排序前 10 个进程
ps aux --sort=-%cpu | head -10    # 按 CPU 排序前 10 个进程

# 查看进程树
pstree -p <pid>                   # 查看进程树

# 杀死进程
kill <pid>                        # 发送 SIGTERM（优雅停止）
kill -9 <pid>                     # 发送 SIGKILL（强制杀死）
kill -3 <pid>                     # 发送 SIGQUIT（生成线程 dump）
pkill -f "java.*app.jar"          # 按名称模式杀死进程

# 后台运行
nohup java -jar app.jar > app.log 2>&1 &    # 后台运行 + 日志重定向
# 2>&1 表示将标准错误重定向到标准输出

# 查看端口占用
lsof -i :8080                     # 查看 8080 端口被谁占用
```

### 三、网络诊断

```bash
# 查看网络连接
netstat -tlnp                     # 查看监听端口（TCP）
ss -tlnp                          # 同上（更快，推荐）
ss -s                             # 连接统计摘要

# 查看 TCP 连接状态分布
ss -ant | awk '{print $1}' | sort | uniq -c | sort -rn

# 网络连通性测试
ping 192.168.1.1                  # ICMP 连通性
telnet 192.168.1.1 3306           # TCP 端口连通性
curl -v http://localhost:8080/api  # HTTP 请求测试
curl -o /dev/null -s -w "%{http_code}" http://localhost:8080/health  # 只看状态码

# DNS 查询
nslookup www.example.com
dig www.example.com

# 路由追踪
traceroute www.example.com

# 抓包
tcpdump -i eth0 port 8080 -w capture.pcap    # 抓取 8080 端口的包
tcpdump -i eth0 host 192.168.1.1 -nn         # 抓取指定 IP 的包
```

### 四、磁盘管理

```bash
# 查看磁盘使用
df -h                             # 查看磁盘分区使用率
du -sh /opt/app/                  # 查看目录大小
du -sh /opt/app/* | sort -rh | head -10  # 目录大小排序

# 查看 inode 使用
df -i                             # inode 耗尽也会导致无法创建文件

# 查看磁盘 IO
iostat -x 1 5                     # 每秒刷新，共 5 次

# 查看大文件
find / -type f -size +500M -exec ls -lh {} \;  # 查找大于 500M 的文件
```

### 五、文本处理

```bash
# grep：文本搜索
grep "ERROR" app.log              # 搜索包含 ERROR 的行
grep -i "error" app.log           # 忽略大小写
grep -n "Exception" app.log       # 显示行号
grep -c "ERROR" app.log           # 统计匹配行数
grep -A 5 "OutOfMemoryError" app.log  # 匹配行及后 5 行
grep -B 3 "ERROR" app.log         # 匹配行及前 3 行
grep -r "TODO" /opt/app/src/      # 递归搜索目录

# sort + uniq：排序去重
cat access.log | awk '{print $1}' | sort | uniq -c | sort -rn | head -10
# 统计访问 IP Top 10

# wc：统计
wc -l app.log                     # 统计行数
grep "ERROR" app.log | wc -l      # 统计错误数
```

### 六、系统信息

```bash
# 系统信息
uname -a                          # 内核版本
cat /etc/os-release               # 系统版本
hostname                          # 主机名

# 内存信息
free -h                           # 内存使用情况
cat /proc/meminfo                 # 详细内存信息

# CPU 信息
lscpu                             # CPU 信息
cat /proc/cpuinfo | grep "model name" | head -1  # CPU 型号
nproc                             # CPU 核心数

# 查看系统负载
uptime                            # 1/5/15 分钟平均负载
w                                 # 登录用户和负载
```

## 常见面试题

### Q1: 如何查看 Java 进程并获取其 PID？

**难度**：⭐ | **频率**：🔥🔥🔥

**标准答案**：

```bash
# 方法一：ps + grep
ps -ef | grep java | grep -v grep

# 方法二：jps（JDK 自带）
jps -l

# 方法三：pgrep
pgrep -a java
```

### Q2: 如何查看某个端口被哪个进程占用？

**难度**：⭐ | **频率**：🔥🔥🔥

**标准答案**：

```bash
# 方法一：lsof
lsof -i :8080

# 方法二：ss
ss -tlnp | grep 8080

# 方法三：netstat
netstat -tlnp | grep 8080
```

### Q3: 如何实时查看日志文件？

**难度**：⭐ | **频率**：🔥🔥

**标准答案**：

```bash
# 实时追踪
tail -f app.log

# 实时追踪并过滤
tail -f app.log | grep ERROR

# 从最后 1000 行开始追踪
tail -n 1000 -f app.log
```

## 参考资料

- [Linux 命令大全](https://man7.org/linux/man-pages/)
- [tldr pages](https://tldr.sh/) — 简化版 man pages
