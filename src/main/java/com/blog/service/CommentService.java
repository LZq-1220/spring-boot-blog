package com.blog.service;

import com.blog.entity.Article;
import com.blog.entity.Comment;
import com.blog.entity.User;
import com.blog.exception.BadRequestException;
import com.blog.exception.ForbiddenException;
import com.blog.exception.NotFoundException;
import com.blog.repository.ArticleRepository;
import com.blog.repository.CommentRepository;
import com.blog.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;

    @Data
    @AllArgsConstructor
    public static class CommentDto {
        private Long id;
        private String content;
        private String username;
        private String nickname;
        private Long parentId;
        private List<CommentDto> replies;
        private LocalDateTime createdAt;

        public static CommentDto fromEntity(Comment comment) {
            return new CommentDto(
                    comment.getId(),
                    comment.getContent(),
                    comment.getUser().getUsername(),
                    comment.getUser().getNickname() != null ? comment.getUser().getNickname() : comment.getUser().getUsername(),
                    comment.getParent() != null ? comment.getParent().getId() : null,
                    new ArrayList<>(),
                    comment.getCreatedAt()
            );
        }
    }

    /**
     * 返回根评论及其回复（两层）。
     * 加 @Transactional 保证 session 存活，回复的 user 也在仓库里 fetch 了。
     * 回复改为按 parentIds 一次查完，避免每条根评论一次查询。
     */
    @Transactional(readOnly = true)
    public List<CommentDto> getArticleComments(Long articleId) {
        List<Comment> rootComments = commentRepository.findRootCommentsByArticleId(articleId);
        if (rootComments.isEmpty()) {
            return List.of();
        }

        List<Long> rootIds = rootComments.stream().map(Comment::getId).toList();
        Map<Long, List<CommentDto>> repliesByParent = commentRepository.findRepliesByParentIds(rootIds)
                .stream()
                .collect(Collectors.groupingBy(
                        r -> r.getParent().getId(),
                        Collectors.mapping(CommentDto::fromEntity, Collectors.toList())));

        return rootComments.stream().map(root -> {
            CommentDto dto = CommentDto.fromEntity(root);
            dto.setReplies(repliesByParent.getOrDefault(root.getId(), List.of()));
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional
    public CommentDto addComment(Long articleId, Long userId, String content, Long parentId) {
        if (content == null || content.isBlank()) {
            throw new BadRequestException("评论内容不能为空");
        }

        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new NotFoundException("文章不存在"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在"));

        Comment comment = Comment.builder()
                .content(content.trim())
                .article(article)
                .user(user)
                .build();

        if (parentId != null) {
            Comment parent = commentRepository.findById(parentId)
                    .orElseThrow(() -> new NotFoundException("父评论不存在"));
            if (!parent.getArticle().getId().equals(articleId)) {
                throw new BadRequestException("父评论不属于该文章");
            }
            // 只支持两层：对回复的回复，挂到它的根评论下，
            // 否则数据存进去了但接口永远查不出来
            comment.setParent(parent.getParent() != null ? parent.getParent() : parent);
        }

        comment = commentRepository.save(comment);
        commentRepository.flush();
        return CommentDto.fromEntity(comment);
    }

    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("评论不存在"));

        if (!comment.getUser().getId().equals(userId)
                && !comment.getArticle().getAuthor().getId().equals(userId)) {
            throw new ForbiddenException("无权删除此评论");
        }

        // 先删子回复，否则 parent_id 外键会阻止删除
        commentRepository.deleteAll(commentRepository.findByParentId(commentId));
        commentRepository.delete(comment);
    }
}
