package com.example.user.entity;

import java.time.LocalDateTime;

/**
 * 用户实体类
 *
 * 对应 users 表，被 Feign 跨服务调用时作为返回值序列化为 JSON。
 */
public class User {

    /** 用户ID */
    private Long id;

    /** 用户名 */
    private String name;

    /** 邮箱 */
    private String email;

    /** 年龄 */
    private Integer age;

    /** 创建时间 */
    private LocalDateTime createTime;

    public User() {
    }

    public User(Long id, String name, String email, Integer age, LocalDateTime createTime) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.age = age;
        this.createTime = createTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", age=" + age +
                ", createTime=" + createTime +
                '}';
    }
}
