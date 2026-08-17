package com.blog.controller;

import com.blog.dto.ArticleRequest;
import com.blog.dto.ArticleResponse;
import com.blog.dto.ArticleSummary;
import com.blog.service.ArticleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @GetMapping
    public ResponseEntity<Page<ArticleSummary>> getArticles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long tagId) {
        return ResponseEntity.ok(articleService.getPublishedArticles(page, size, categoryId, tagId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArticleResponse> getArticle(@PathVariable Long id) {
        return ResponseEntity.ok(articleService.getArticleById(id));
    }

    @PostMapping
    public ResponseEntity<ArticleResponse> createArticle(
            @Valid @RequestBody ArticleRequest request,
            Authentication auth) {
        return ResponseEntity.ok(articleService.createArticle(request, auth.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ArticleResponse> updateArticle(
            @PathVariable Long id,
            @Valid @RequestBody ArticleRequest request,
            Authentication auth) {
        return ResponseEntity.ok(articleService.updateArticle(id, request, auth.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteArticle(
            @PathVariable Long id,
            Authentication auth) {
        articleService.deleteArticle(id, auth.getName());
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }

    /**
     * 我的文章。不传 status 时同时返回草稿和已发布，
     * 后台需要能看到草稿。
     */
    @GetMapping("/my")
    public ResponseEntity<Page<ArticleResponse>> getMyArticles(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(articleService.getMyArticles(auth.getName(), page, size, status));
    }
}
