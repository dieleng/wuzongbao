package com.example.portfolio.web.dto;

import com.example.portfolio.entity.Project;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 后台项目编辑表单
 */
public class ProjectForm {

    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题最长 100 字")
    private String title;

    private String summary;

    private String tags;

    private String coverUrl;

    private String content;

    private String demoUrl;

    private String repoUrl;

    private Boolean published;

    private Integer sortWeight;

    /** 新建时使用 */
    public Project toEntity() {
        Project p = new Project();
        applyTo(p);
        return p;
    }

    /** 编辑时使用 */
    public void applyTo(Project p) {
        p.setTitle(title.trim());
        p.setSummary(trimToNull(summary));
        p.setTags(trimToNull(tags));
        p.setCoverUrl(trimToNull(coverUrl));
        p.setContent(trimToNull(content));
        p.setDemoUrl(trimToNull(demoUrl));
        p.setRepoUrl(trimToNull(repoUrl));
        p.setPublished(published == null || published);
        p.setSortWeight(sortWeight == null ? 0 : sortWeight);
    }

    private String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    // ---------- getters / setters ----------

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

    public Boolean getPublished() { return published; }

    public void setPublished(Boolean published) { this.published = published; }

    public Integer getSortWeight() { return sortWeight; }

    public void setSortWeight(Integer sortWeight) { this.sortWeight = sortWeight; }
}
