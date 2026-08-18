package com.blog.dto;

import com.blog.entity.Tag;

/**
 * 标签及其关联文章数量的 DTO
 * 用于热门标签、相关标签推荐等场景
 */
public record TagWithCountDto(Long id, String name, Long articleCount) {

    public static TagWithCountDto fromEntityAndCount(Tag tag, Long count) {
        return new TagWithCountDto(tag.getId(), tag.getName(), count);
    }
}
