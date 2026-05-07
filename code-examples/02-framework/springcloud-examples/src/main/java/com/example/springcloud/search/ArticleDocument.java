package com.example.springcloud.search;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 10.A.20 Elasticsearch 文章文档实体
 *
 * <p>映射到 Elasticsearch 的 articles 索引。
 */
@Document(indexName = "articles")
public class ArticleDocument {

    /** 文档 ID */
    @Id
    private String id;

    /** 文章标题 */
    @Field(type = FieldType.Text)
    private String title;

    /** 文章内容 */
    @Field(type = FieldType.Text)
    private String content;

    /** 作者 */
    @Field(type = FieldType.Keyword)
    private String author;

    /** 标签列表 */
    @Field(type = FieldType.Keyword)
    private List<String> tags;

    /** 创建时间 */
    @Field(type = FieldType.Date)
    private LocalDateTime createTime;

    public ArticleDocument() {
    }

    public ArticleDocument(String id, String title, String content, String author,
                           List<String> tags, LocalDateTime createTime) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.author = author;
        this.tags = tags;
        this.createTime = createTime;
    }

    // getter/setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
