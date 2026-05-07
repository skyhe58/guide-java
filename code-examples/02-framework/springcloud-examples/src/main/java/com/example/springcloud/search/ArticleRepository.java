package com.example.springcloud.search;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

/**
 * 10.A.20 Elasticsearch 文章仓库
 *
 * <p>继承 ElasticsearchRepository，提供基本 CRUD 和自定义查询方法。
 */
public interface ArticleRepository extends ElasticsearchRepository<ArticleDocument, String> {

    /**
     * 按标题模糊搜索文章
     *
     * @param keyword 搜索关键词
     * @return 匹配的文章列表
     */
    List<ArticleDocument> findByTitleContaining(String keyword);
}
