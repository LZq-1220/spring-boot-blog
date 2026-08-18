package com.blog.repository;

import com.blog.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByName(String name);

    /**
     * 模糊搜索标签名称（类似抖音输入 #Java 搜索相关标签）
     * 使用 LOWER 做大小写不敏感匹配
     */
    @Query("SELECT t FROM Tag t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Tag> searchByName(@Param("keyword") String keyword);

    /**
     * 查询热门标签（按关联文章数量倒序）
     * 返回 [Tag实体, 文章数] 的二元组列表
     */
    @Query("SELECT t, COUNT(a) FROM Tag t LEFT JOIN t.articles a " +
           "WHERE a.status = com.blog.entity.Article$Status.PUBLISHED OR a IS NULL " +
           "GROUP BY t ORDER BY COUNT(a) DESC")
    List<Object[]> findHotTags();

    /**
     * 统计所有标签及其关联的已发布文章数
     * 返回 [Tag实体, 文章数]
     */
    @Query("SELECT t, COUNT(a) FROM Tag t LEFT JOIN t.articles a " +
           "WHERE a.status = com.blog.entity.Article$Status.PUBLISHED OR a IS NULL " +
           "GROUP BY t ORDER BY t.name ASC")
    List<Object[]> findAllWithArticleCount();

    /**
     * 查找与指定标签相关的其他标签（共现标签）
     * 原理：查找包含相同标签的文章中的其他标签
     */
    @Query("SELECT DISTINCT t, COUNT(a) FROM Tag t " +
           "JOIN t.articles a " +
           "WHERE a.id IN (" +
           "  SELECT a2.id FROM Article a2 JOIN a2.tags t2 WHERE t2.id = :tagId" +
           ") " +
           "AND t.id <> :tagId " +
           "AND a.status = com.blog.entity.Article$Status.PUBLISHED " +
           "GROUP BY t ORDER BY COUNT(a) DESC")
    List<Object[]> findRelatedTags(@Param("tagId") Long tagId);
}
