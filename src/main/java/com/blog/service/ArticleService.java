package com.blog.service;

import com.blog.dto.ArticleRequest;
import com.blog.dto.ArticleResponse;
import com.blog.dto.ArticleSummary;
import com.blog.entity.Article;
import com.blog.entity.Category;
import com.blog.entity.User;
import com.blog.exception.BadRequestException;
import com.blog.exception.ForbiddenException;
import com.blog.exception.NotFoundException;
import com.blog.repository.ArticleRepository;
import com.blog.repository.CategoryRepository;
import com.blog.repository.CommentRepository;
import com.blog.repository.TagRepository;
import com.blog.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;

    /** 一次性取回本页所有文章的评论数，避免逐条 count。 */
    private Map<Long, Long> commentCounts(List<Article> articles) {
        if (articles.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = articles.stream().map(Article::getId).toList();
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : commentRepository.countByArticleIdIn(ids)) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }

    @Transactional(readOnly = true)
    public Page<ArticleSummary> getPublishedArticles(int page, int size, Long categoryId, Long tagId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Article> articles;

        if (categoryId != null) {
            if (!categoryRepository.existsById(categoryId)) {
                throw new NotFoundException("分类不存在");
            }
            articles = articleRepository.findByCategoryIdAndStatus(categoryId, Article.Status.PUBLISHED, pageable);
        } else if (tagId != null) {
            if (!tagRepository.existsById(tagId)) {
                throw new NotFoundException("标签不存在");
            }
            articles = articleRepository.findByTagIdAndStatus(tagId, Article.Status.PUBLISHED, pageable);
        } else {
            articles = articleRepository.findByStatus(Article.Status.PUBLISHED, pageable);
        }

        Map<Long, Long> counts = commentCounts(articles.getContent());
        return articles.map(a -> ArticleSummary.fromEntity(a, counts.getOrDefault(a.getId(), 0L)));
    }

    @Transactional
    public ArticleResponse getArticleById(Long id) {
        Article article = articleRepository.findByIdWithAll(id);
        if (article == null) {
            throw new NotFoundException("文章不存在");
        }
        // 原子自增，替代原来的读改写（并发下会丢更新）。
        // 注意不能再对实体 setViewCount：那会让实体变脏，
        // 提交时既触发 @PreUpdate 污染 updatedAt，
        // 又会用绝对值覆盖掉上面的原子自增。
        articleRepository.incrementViewCount(id);

        long commentCount = commentRepository.countByArticleId(id);
        ArticleResponse response = ArticleResponse.fromEntity(article, commentCount);
        // 只在响应里体现自增后的值，不改实体
        response.setViewCount(article.getViewCount() + 1);
        return response;
    }

    @Transactional
    public ArticleResponse createArticle(ArticleRequest request, String username) {
        User author = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("用户不存在"));

        Article article = Article.builder()
                .title(request.getTitle())
                .summary(request.getSummary())
                .content(request.getContent())
                .status(parseStatus(request.getStatus()))
                .author(author)
                .build();

        applyCategoryAndTags(article, request, true);

        article = articleRepository.save(article);
        articleRepository.flush();
        return ArticleResponse.fromEntity(article, 0L);
    }

    @Transactional
    public ArticleResponse updateArticle(Long id, ArticleRequest request, String username) {
        Article article = articleRepository.findByIdWithAll(id);
        if (article == null) {
            throw new NotFoundException("文章不存在");
        }
        if (!article.getAuthor().getUsername().equals(username)) {
            throw new ForbiddenException("只能编辑自己的文章");
        }

        article.setTitle(request.getTitle());
        article.setSummary(request.getSummary());
        article.setContent(request.getContent());
        article.setStatus(parseStatus(request.getStatus()));

        applyCategoryAndTags(article, request, false);

        article = articleRepository.save(article);
        articleRepository.flush();
        return ArticleResponse.fromEntity(article, commentRepository.countByArticleId(id));
    }

    @Transactional
    public void deleteArticle(Long id, String username) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("文章不存在"));

        if (!article.getAuthor().getUsername().equals(username)) {
            throw new ForbiddenException("只能删除自己的文章");
        }

        // 先删回复再删根评论，最后删文章；顺序不能反（parent_id 外键）
        commentRepository.deleteRepliesByArticleId(id);
        commentRepository.deleteAllByArticleId(id);
        articleRepository.delete(article);
    }

    /**
     * status 为 null 时兜底 DRAFT。请求体里的取值由 @Pattern 兜住，
     * 但查询参数没有校验，所以这里也要防非法值（否则 valueOf 抛 IAE 变 500）。
     */
    private Article.Status parseStatus(String status) {
        if (status == null) {
            return Article.Status.DRAFT;
        }
        try {
            return Article.Status.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("status 必须是 DRAFT 或 PUBLISHED");
        }
    }

    private void applyCategoryAndTags(Article article, ArticleRequest request, boolean creating) {
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new NotFoundException("分类不存在"));
            article.setCategory(category);
        } else if (!creating) {
            article.setCategory(null);
        }

        if (request.getTagIds() != null) {
            // clear + addAll 而不是 setTags(new HashSet<>())：
            // 保留 Hibernate 的托管集合实例，避免替换引用带来的脏检查问题
            article.getTags().clear();
            article.getTags().addAll(tagRepository.findAllById(request.getTagIds()));
        }
    }

    /**
     * 「我的文章」。status 为 null 时返回草稿 + 已发布，
     * 后台才能看到并编辑草稿。
     */
    @Transactional(readOnly = true)
    public Page<ArticleResponse> getMyArticles(String username, int page, int size, String status) {
        User author = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("用户不存在"));

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Article> articles = status == null
                ? articleRepository.findByAuthor(author, pageable)
                : articleRepository.findByAuthorAndStatus(author, parseStatus(status), pageable);

        Map<Long, Long> counts = commentCounts(articles.getContent());
        return articles.map(a -> ArticleResponse.fromEntity(a, counts.getOrDefault(a.getId(), 0L)));
    }
}
