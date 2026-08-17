package com.blog.controller;

import com.blog.security.AuthPrincipal;
import com.blog.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/articles/{articleId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping
    public ResponseEntity<List<CommentService.CommentDto>> getComments(@PathVariable Long articleId) {
        return ResponseEntity.ok(commentService.getArticleComments(articleId));
    }

    @PostMapping
    public ResponseEntity<CommentService.CommentDto> addComment(
            @PathVariable Long articleId,
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        String content = (String) body.get("content");
        Long parentId = body.get("parentId") != null ? ((Number) body.get("parentId")).longValue() : null;

        return ResponseEntity.ok(
                commentService.addComment(articleId, AuthPrincipal.userIdOf(auth), content, parentId));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Map<String, String>> deleteComment(
            @PathVariable Long articleId,
            @PathVariable Long commentId,
            Authentication auth) {
        commentService.deleteComment(commentId, AuthPrincipal.userIdOf(auth));
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }
}
