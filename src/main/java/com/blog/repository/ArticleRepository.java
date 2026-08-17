package com.blog.repository;

import com.blog.entity.Article;
import com.blog.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    /*
     * 列表查询统一 LEFT JOIN FETCH author/category：
     * 只 fetch 到一端不会让结果行翻倍，所以分页仍在数据库侧完成。
     * 因为带了 fetch，必须显式给 countQuery。
     * ORDER BY 交给 Pageable 的 Sort，不写死在 JPQL 里（否则会和 Sort 拼出重复的 ORDER BY）。
     */

    @Query(value = "SELECT a FROM Article a LEFT JOIN FETCH a.author LEFT JOIN FETCH a.category"
            + " WHERE a.status = :status",
           countQuery = "SELECT COUNT(a) FROM Article a WHERE a.status = :status")
    Page<Article> findByStatus(@Param("status") Article.Status status, Pageable pageable);

    @Query(value = "SELECT a FROM Article a LEFT JOIN FETCH a.author LEFT JOIN FETCH a.category"
            + " WHERE a.status = :status AND a.category.id = :categoryId",
           countQuery = "SELECT COUNT(a) FROM Article a WHERE a.status = :status AND a.category.id = :categoryId")
    Page<Article> findByCategoryIdAndStatus(@Param("categoryId") Long categoryId,
                                            @Param("status") Article.Status status,
                                            Pageable pageable);

    @Query(value = "SELECT a FROM Article a LEFT JOIN FETCH a.author LEFT JOIN FETCH a.category"
            + " JOIN a.tags t WHERE t.id = :tagId AND a.status = :status",
           countQuery = "SELECT COUNT(a) FROM Article a JOIN a.tags t WHERE t.id = :tagId AND a.status = :status")
    Page<Article> findByTagIdAndStatus(@Param("tagId") Long tagId,
                                       @Param("status") Article.Status status,
                                       Pageable pageable);

    /** 「我的文章」：原先漏了 author 条件，返回的是全站文章。 */
    @Query(value = "SELECT a FROM Article a LEFT JOIN FETCH a.author LEFT JOIN FETCH a.category"
            + " WHERE a.author = :author",
           countQuery = "SELECT COUNT(a) FROM Article a WHERE a.author = :author")
    Page<Article> findByAuthor(@Param("author") User author, Pageable pageable);

    @Query(value = "SELECT a FROM Article a LEFT JOIN FETCH a.author LEFT JOIN FETCH a.category"
            + " WHERE a.author = :author AND a.status = :status",
           countQuery = "SELECT COUNT(a) FROM Article a WHERE a.author = :author AND a.status = :status")
    Page<Article> findByAuthorAndStatus(@Param("author") User author,
                                        @Param("status") Article.Status status,
                                        Pageable pageable);

    /** 详情页：tags 是集合，单条记录 fetch 不涉及分页，安全。 */
    @Query("SELECT a FROM Article a LEFT JOIN FETCH a.author LEFT JOIN FETCH a.category"
            + " LEFT JOIN FETCH a.tags WHERE a.id = :id")
    Article findByIdWithAll(@Param("id") Long id);

    /** 原子自增，避免读改写丢更新。 */
    @Modifying
    @Query("UPDATE Article a SET a.viewCount = a.viewCount + 1 WHERE a.id = :id")
    void incrementViewCount(@Param("id") Long id);
}
