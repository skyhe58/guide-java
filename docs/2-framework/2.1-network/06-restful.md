---
title: "RESTful API 设计"
module: "network"
difficulty: "intermediate"
interviewFrequency: "high"
tags:
  - "RESTful"
  - "API设计"
  - "HTTP方法"
  - "面试高频"
codeExample: "02-framework/network-programming/src/main/java/com/example/network/http/"
relatedEntries:
  - "/2-framework/2.1-network/02-http"
  - "/2-framework/2.1-network/07-rpc"
  - "/2-framework/2.2-springboot/web"
prerequisites:
  - "/2-framework/2.1-network/02-http"
estimatedTime: "40min"
---

# RESTful API 设计

## 概念说明

REST（Representational State Transfer，表述性状态转移）是一种软件架构风格，由 Roy Fielding 在 2000 年的博士论文中提出。RESTful API 是遵循 REST 原则设计的 Web API，以资源为中心，通过 HTTP 方法表达操作语义，是目前最主流的 API 设计风格。

## 核心原理

### 一、REST 六大约束

| 约束 | 说明 |
|------|------|
| 客户端-服务端分离 | 前后端职责分离，独立演进 |
| 无状态 | 每个请求包含所有必要信息，服务端不保存会话状态 |
| 可缓存 | 响应应标明是否可缓存，减少不必要的交互 |
| 统一接口 | 资源标识、资源操作、自描述消息、HATEOAS |
| 分层系统 | 客户端无法感知是否直连服务端（可能经过代理/网关） |
| 按需代码（可选） | 服务端可以返回可执行代码（如 JavaScript） |

### 二、URL 设计规范

```
# ✅ 好的 URL 设计（名词、复数、层级清晰）
GET    /api/v1/users              # 获取用户列表
GET    /api/v1/users/123          # 获取单个用户
POST   /api/v1/users              # 创建用户
PUT    /api/v1/users/123          # 全量更新用户
PATCH  /api/v1/users/123          # 部分更新用户
DELETE /api/v1/users/123          # 删除用户
GET    /api/v1/users/123/orders   # 获取用户的订单列表

# ❌ 不好的 URL 设计（动词、不规范）
GET    /api/getUser?id=123
POST   /api/deleteUser
GET    /api/user/list
POST   /api/updateUserInfo
```

**URL 设计原则**：
1. 使用**名词**表示资源，不使用动词
2. 使用**复数**形式（`/users` 而非 `/user`）
3. 使用**层级关系**表示资源从属（`/users/123/orders`）
4. 使用**连字符** `-` 分隔单词（`/user-profiles`）
5. URL 全部**小写**

### 三、HTTP 方法语义

| 方法 | 语义 | 幂等 | 安全 | 请求体 | 典型响应码 |
|------|------|------|------|--------|-----------|
| GET | 获取资源 | ✅ | ✅ | 无 | 200 |
| POST | 创建资源 | ❌ | ❌ | 有 | 201 |
| PUT | 全量替换 | ✅ | ❌ | 有 | 200 |
| PATCH | 部分更新 | ❌ | ❌ | 有 | 200 |
| DELETE | 删除资源 | ✅ | ❌ | 可选 | 204 |

> ⚠️ **幂等性**：同一请求执行多次，结果与执行一次相同。GET、PUT、DELETE 是幂等的，POST 不是。

### 四、状态码使用规范

| 场景 | 状态码 | 说明 |
|------|--------|------|
| 查询成功 | 200 OK | 返回资源数据 |
| 创建成功 | 201 Created | 返回新资源，Location 头指向新资源 URL |
| 删除成功 | 204 No Content | 无响应体 |
| 参数错误 | 400 Bad Request | 请求格式或参数不合法 |
| 未认证 | 401 Unauthorized | 需要登录 |
| 无权限 | 403 Forbidden | 已登录但无权限 |
| 资源不存在 | 404 Not Found | 资源不存在 |
| 方法不允许 | 405 Method Not Allowed | 不支持该 HTTP 方法 |
| 冲突 | 409 Conflict | 资源冲突（如重复创建） |
| 服务器错误 | 500 Internal Server Error | 服务端异常 |

### 五、版本控制

| 方式 | 示例 | 优缺点 |
|------|------|--------|
| URL 路径 | `/api/v1/users` | ✅ 直观清晰 ❌ URL 变化 |
| 请求头 | `Accept: application/vnd.api.v1+json` | ✅ URL 不变 ❌ 不直观 |
| 查询参数 | `/api/users?version=1` | ✅ 简单 ❌ 不够规范 |

> 推荐使用 **URL 路径版本控制**（`/api/v1/`），最直观且被广泛采用。

### 六、HATEOAS

HATEOAS（Hypermedia As The Engine Of Application State）是 REST 的最高成熟度级别，响应中包含相关操作的链接：

```json
{
  "id": 123,
  "name": "张三",
  "email": "[email]",
  "_links": {
    "self": { "href": "/api/v1/users/123" },
    "orders": { "href": "/api/v1/users/123/orders" },
    "update": { "href": "/api/v1/users/123", "method": "PUT" },
    "delete": { "href": "/api/v1/users/123", "method": "DELETE" }
  }
}
```

### 七、统一响应格式

```json
// 成功响应
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 123,
    "name": "张三"
  }
}

// 分页响应
{
  "code": 200,
  "message": "success",
  "data": {
    "items": [...],
    "total": 100,
    "page": 1,
    "pageSize": 10
  }
}

// 错误响应
{
  "code": 400,
  "message": "参数校验失败",
  "errors": [
    { "field": "email", "message": "邮箱格式不正确" }
  ]
}
```

## 代码示例

```java
// Spring Boot RESTful Controller 示例
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @GetMapping
    public ResponseEntity<List<User>> list() { ... }

    @GetMapping("/{id}")
    public ResponseEntity<User> get(@PathVariable Long id) { ... }

    @PostMapping
    public ResponseEntity<User> create(@RequestBody User user) {
        User created = userService.create(user);
        return ResponseEntity.created(
            URI.create("/api/v1/users/" + created.getId()))
            .body(created);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

> 💻 完整可运行代码：[HttpDemo.java](https://github.com/skyhe58/guide-java/tree/main/code-examples/02-framework/network-programming/src/main/java/com/example/network/http/HttpDemo.java)
> <!-- 本地路径：code-examples/02-framework/network-programming/src/main/java/com/example/network/http/HttpDemo.java -->

## 常见面试题

### Q1: RESTful API 的设计原则是什么？

**难度**：⭐⭐ | **频率**：🔥🔥🔥

**答题思路**：

1. 以资源为中心，URL 用名词
2. HTTP 方法表达操作语义
3. 状态码表达结果

**标准答案**：

RESTful API 以资源为中心，URL 使用名词复数形式标识资源（如 `/users`），通过 HTTP 方法表达操作语义（GET 查询、POST 创建、PUT 更新、DELETE 删除），使用标准 HTTP 状态码表达结果（200 成功、201 创建、404 不存在等）。核心原则包括无状态、统一接口、可缓存。

**深入追问**：

- PUT 和 PATCH 的区别？（全量替换 vs 部分更新）
- 如何设计批量操作的 API？（`POST /users/batch` 或 `DELETE /users?ids=1,2,3`）
- 如何处理 API 版本升级？

### Q2: 什么是幂等性？哪些 HTTP 方法是幂等的？

**难度**：⭐⭐⭐ | **频率**：🔥🔥🔥

**标准答案**：

幂等性是指同一个请求执行一次和执行多次的效果相同。GET、PUT、DELETE 是幂等的：GET 多次获取同一资源结果相同；PUT 多次全量替换结果相同；DELETE 多次删除同一资源结果相同（第一次删除成功，后续返回 404）。POST 不是幂等的，多次提交会创建多个资源。

**深入追问**：

- 如何保证 POST 接口的幂等性？（幂等 Token、唯一索引、状态机）
- PATCH 是幂等的吗？（取决于实现，规范上不要求幂等）

### Q3: REST 和 RPC 的区别？如何选择？

**难度**：⭐⭐⭐ | **频率**：🔥🔥

**标准答案**：

REST 以资源为中心，通过 HTTP 方法操作资源，适合对外暴露的 API（如开放平台、前后端交互）。RPC 以操作为中心，直接调用远程方法，适合内部微服务间的高性能通信。REST 更通用、更易理解，RPC 性能更高（二进制序列化、长连接）。选择原则：对外用 REST，内部高性能场景用 RPC。

**深入追问**：

- GraphQL 和 REST 的区别？
- gRPC 的优势是什么？

## 参考资料

- [Roy Fielding 的 REST 论文](https://www.ics.uci.edu/~fielding/pubs/dissertation/rest_arch_style.htm)
- [RESTful API 设计最佳实践](https://restfulapi.net/)
- [Microsoft REST API Guidelines](https://github.com/microsoft/api-guidelines)
