package com.example.springcloud.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * 10.A.21 MongoDB 用户仓库
 *
 * <p>继承 MongoRepository，提供基本 CRUD 和自定义查询方法。
 */
public interface UserMongoRepository extends MongoRepository<UserDocument, String> {

    /**
     * 按名称模糊查询用户
     *
     * @param name 用户名关键词
     * @return 匹配的用户列表
     */
    List<UserDocument> findByNameContaining(String name);
}
