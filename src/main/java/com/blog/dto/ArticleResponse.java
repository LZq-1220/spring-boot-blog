package com.blog.dto;

import com.blog.entity.Article;
import com.blog.entity.Tag;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
public class ArticleResponse {
    private Long id;
    private String title;
    private String summary;
    private String content;
    private String status;
    private Integer viewCount;
    private String authorName;
    private String authorNickname;
    private String categoryName;
    private Long categoryId;
    private Set<String> tags;
    private Set<Long> tagIds;
    private Long commentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * commentCount 由调用方传入，理由同 ArticleSummary。
     * 调用方需保证 author / category / tags 已 fetch 或仍在事务内。
     */
    public static ArticleResponse fromEntity(Article article, long commentCount) {
        return new ArticleResponse(
            article.getId(),
            article.getTitle(),
            article.getSummary(),
            article.getContent(),
            article.getStatus().name(),
            article.getViewCount(),
            article.getAuthor().getUsername(),
            article.getAuthor().getNickname() != null ? article.getAuthor().getNickname() : article.getAuthor().getUsername(),
            article.getCategory() != null ? article.getCategory().getName() : null,
            article.getCategory() != null ? article.getCategory().getId() : null,
            article.getTags().stream().map(Tag::getName).collect(Collectors.toSet()),
            article.getTags().stream().map(Tag::getId).collect(Collectors.toSet()),
            commentCount,
            article.getCreatedAt(),
            article.getUpdatedAt()
        );
    }
}
