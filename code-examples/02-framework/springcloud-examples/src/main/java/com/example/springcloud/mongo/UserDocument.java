package com.example.springcloud.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 10.A.21 MongoDB 用户文档实体
 *
 * <p>映射到 MongoDB 的 users 集合。
 */
@Document(collection = "users")
public class UserDocument {

    /** 文档 ID */
    @Id
    private String id;

    /** 用户名 */
    private String name;

    /** 邮箱 */
    private String email;

    /** 年龄 */
    private Integer age;

    /** 技能列表 */
    private List<String> skills;

    /** 创建时间 */
    private LocalDateTime createTime;

    public UserDocument() {
    }

    public UserDocument(String id, String name, String email, Integer age,
                        List<String> skills, LocalDateTime createTime) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.age = age;
        this.skills = skills;
        this.createTime = createTime;
    }

    // getter/setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
