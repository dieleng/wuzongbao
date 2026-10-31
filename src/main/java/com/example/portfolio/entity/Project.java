package com.example.portfolio.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "project")
public class Project {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    /** 一句话简介,列表卡片上展示 */
    @Column(length = 300)
    private String summary;

    /** 技术栈标签,英文逗号分隔,如: Java,Netty,Redis */
    @Column(length = 200)
    private String tags;

    /** 封面图 URL(上传接口返回的相对路径) */
    @Column(length = 500)
    private String coverUrl;

    /** 详细介绍,纯文本,按换行展示 */
    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @Column(length = 300)
    private String demoUrl;

    @Column(length = 300)
    private String repoUrl;

    /** false = 草稿,前台不显示 */
    @Column(nullable = false)
    private boolean published = true;

    /** 排序权重,越大越靠前 */
    @Column(nullable = false)
    private int sortWeight;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /** 页面展示用的格式化时间 */
    public String getCreatedAtText() {
        return createdAt == null ? "" : createdAt.format(FMT);
    }

    public String getUpdatedAtText() {
        return updatedAt == null ? "" : updatedAt.format(FMT);
    }

    // ---------- getters / setters ----------

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }

    public void setTitle(String title) { this.title = title; }

    public String getSummary() { return summary; }

    public void setSummary(String summary) { this.summary = summary; }

    public String getTags() { return tags; }

    public void setTags(String tags) { this.tags = tags; }

    public String getCoverUrl() { return coverUrl; }

    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public String getContent() { return content; }

    public void setContent(String content) { this.content = content; }

    public String getDemoUrl() { return demoUrl; }

    public void setDemoUrl(String demoUrl) { this.demoUrl = demoUrl; }

    public String getRepoUrl() { return repoUrl; }

    public void setRepoUrl(String repoUrl) { this.repoUrl = repoUrl; }

    public boolean isPublished() { return published; }

    public void setPublished(boolean published) { this.published = published; }

    public int getSortWeight() { return sortWeight; }

    public void setSortWeight(int sortWeight) { this.sortWeight = sortWeight; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
