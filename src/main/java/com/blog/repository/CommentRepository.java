package com.blog.repository;

import com.blog.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("SELECT c FROM Comment c JOIN FETCH c.user WHERE c.article.id = :articleId"
            + " AND c.parent IS NULL ORDER BY c.createdAt DESC")
    List<Comment> findRootCommentsByArticleId(@Param("articleId") Long articleId);

    /** 加了 JOIN FETCH c.user：原先没 fetch，DTO 里读 user 会抛 LazyInitializationException。 */
    @Query("SELECT c FROM Comment c JOIN FETCH c.user WHERE c.parent.id IN :parentIds"
            + " ORDER BY c.createdAt ASC")
    List<Comment> findRepliesByParentIds(@Param("parentIds") Collection<Long> parentIds);

    long countByArticleId(Long articleId);

    /** 批量统计，避免文章列表逐条查评论数造成 N+1。返回 [articleId, count]。 */
    @Query("SELECT c.article.id, COUNT(c) FROM Comment c WHERE c.article.id IN :articleIds"
            + " GROUP BY c.article.id")
    List<Object[]> countByArticleIdIn(@Param("articleIds") Collection<Long> articleIds);

    List<Comment> findByParentId(Long parentId);

    /*
     * 删文章时必须「先子后父」：comments.parent_id 是自引用外键，
     * 交给 cascade 按任意顺序删会撞外键约束。
     * 当前业务只有两层评论（接口也只返回两层），故两步足够。
     */

    @Modifying
    @Query("DELETE FROM Comment c WHERE c.article.id = :articleId AND c.parent IS NOT NULL")
    void deleteRepliesByArticleId(@Param("articleId") Long articleId);

    @Modifying
    @Query("DELETE FROM Comment c WHERE c.article.id = :articleId")
    void deleteAllByArticleId(@Param("articleId") Long articleId);
}
