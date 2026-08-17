package com.blog.dto;

import com.blog.entity.Article;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ArticleSummary {
    private Long id;
    private String title;
    private String summary;
    private String status;
    private Integer viewCount;
    private String authorNickname;
    private String categoryName;
    private Long commentCount;
    private LocalDateTime createdAt;

    /**
     * commentCount 由调用方用 count 查询传入：原先读 article.getComments().size()
     * 会把整个评论集合加载出来，且在事务外必定抛 LazyInitializationException。
     * 调用方需保证 author / category 已被 fetch 或仍处于事务中。
     */
    public static ArticleSummary fromEntity(Article article, long commentCount) {
        return new ArticleSummary(
            article.getId(),
            article.getTitle(),
            article.getSummary(),
            article.getStatus().name(),
            article.getViewCount(),
            article.getAuthor().getNickname() != null ? article.getAuthor().getNickname() : article.getAuthor().getUsername(),
            article.getCategory() != null ? article.getCategory().getName() : null,
            commentCount,
            article.getCreatedAt()
        );
    }
}
