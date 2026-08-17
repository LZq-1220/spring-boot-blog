package com.blog.dto;

import com.blog.entity.Tag;

/** 同 CategoryDto，避免序列化懒加载的 articles 集合。 */
public record TagDto(Long id, String name) {

    public static TagDto fromEntity(Tag tag) {
        return new TagDto(tag.getId(), tag.getName());
    }
}
