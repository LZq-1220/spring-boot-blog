package com.blog.dto;

import com.blog.entity.Category;

/**
 * 只暴露 id + name。直接返回 Category 实体会让 Jackson 去序列化懒加载的
 * articles 集合（session 已关 -> 500），且 Article <-> Category 会无限递归。
 */
public record CategoryDto(Long id, String name) {

    public static CategoryDto fromEntity(Category category) {
        return new CategoryDto(category.getId(), category.getName());
    }
}
