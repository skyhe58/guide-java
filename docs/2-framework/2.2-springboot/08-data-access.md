---
title: "数据访问"
module: "springboot"
difficulty: "intermediate"
interviewFrequency: "medium"
tags:
  - "JPA"
  - "MyBatis"
  - "MyBatis-Plus"
  - "Spring Data"
  - "面试中频"
codeExample: "02-framework/springboot-examples/src/main/java/com/example/springboot/"
relatedEntries:
  - "/3-data-store/3.1-database/"
  - "/2-framework/2.2-springboot/02-aop"
prerequisites:
  - "/2-framework/2.2-springboot/06-config-files"
estimatedTime: "45min"
---

# 数据访问

## 概念说明

Spring Boot 提供了多种数据访问方案，最常用的是 JPA（Spring Data JPA）和 MyBatis（MyBatis-Plus）。选择哪种取决于项目需求和团队习惯。

## 核心原理

### 一、JPA vs MyBatis vs MyBatis-Plus 对比

| 特性 | Spring Data JPA | MyBatis | MyBatis-Plus |
|------|----------------|---------|-------------|
| 理念 | ORM，对象关系映射 | SQL 映射 | MyBatis 增强 |
| SQL 控制 | 自动生成，可自定义 | 完全手写 | 简单 CRUD 自动，复杂手写 |
| 学习曲线 | 较陡（JPA 规范） | 较平缓 | 平缓 |
| 适合场景 | 简单 CRUD、快速开发 | 复杂 SQL、性能要求高 | 大多数业务场景 |
| 国内使用率 | 中等 | 高 | 非常高 |

### 二、Spring Data JPA

```java
// 实体类
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String email;
}

// Repository 接口 — 方法名自动生成 SQL
public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByUsername(String username);
    List<User> findByEmailContaining(String email);

    @Query("SELECT u FROM User u WHERE u.username = :name")
    User findByCustomQuery(@Param("name") String name);
}
```

### 三、MyBatis 动态 SQL

```xml
<select id="findUsers" resultType="User">
    SELECT * FROM users
    <where>
        <if test="username != null">
            AND username LIKE CONCAT('%', #{username}, '%')
        </if>
        <if test="email != null">
            AND email = #{email}
        </if>
    </where>
    <if test="orderBy != null">
        ORDER BY ${orderBy}
    </if>
</select>
```

### 四、MyBatis-Plus 分页插件

```java
// 配置分页插件
@Configuration
public class MybatisPlusConfig {
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}

// 使用分页
Page<User> page = new Page<>(1, 10);
LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
        .like(User::getUsername, "test")
        .orderByDesc(User::getCreateTime);
userMapper.selectPage(page, wrapper);
```

## 代码示例

> 💻 完整可运行代码：[SpringBootApp.java](https://github.com/skyhe58/guide-java/tree/main/code-examples/02-framework/springboot-examples/src/main/java/com/example/springboot/SpringBootApp.java)
> <!-- 本地路径：code-examples/02-framework/springboot-examples/src/main/java/com/example/springboot/SpringBootApp.java -->

## 常见面试题

### Q1: JPA 和 MyBatis 怎么选？

**难度**：⭐⭐ | **频率**：🔥🔥

**标准答案**：

JPA 适合简单 CRUD 和快速开发，自动生成 SQL 减少样板代码；MyBatis 适合复杂 SQL 和对性能有要求的场景，SQL 完全可控。国内大多数项目使用 MyBatis-Plus，兼顾了开发效率和 SQL 控制力。

### Q2: MyBatis 的 #{} 和 ${} 的区别？

**难度**：⭐⭐ | **频率**：🔥🔥🔥

**标准答案**：

`#{}` 是预编译参数，会使用 PreparedStatement 的 `?` 占位符，防止 SQL 注入；`${}` 是字符串替换，直接拼接到 SQL 中，有 SQL 注入风险。一般用 `#{}` 传值，`${}` 只用于动态表名、列名等不能用占位符的场景。

### Q3: MyBatis-Plus 的分页原理？

**难度**：⭐⭐ | **频率**：🔥🔥

**标准答案**：

MyBatis-Plus 通过拦截器（PaginationInnerInterceptor）实现分页。它拦截 SQL 执行，自动在原始 SQL 后追加 `LIMIT offset, size`，并额外执行一条 `SELECT COUNT(*)` 查询获取总记录数。

## 参考资料

- [Spring Data JPA 官方文档](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [MyBatis-Plus 官方文档](https://baomidou.com/)
