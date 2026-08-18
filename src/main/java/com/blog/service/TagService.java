package com.blog.service;

import com.blog.dto.TagDto;
import com.blog.dto.TagWithCountDto;
import com.blog.entity.Tag;
import com.blog.exception.NotFoundException;
import com.blog.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;

    /**
     * 模糊搜索标签（类似抖音搜索 #Java）
     * 返回匹配的标签列表
     */
    @Transactional(readOnly = true)
    public List<TagDto> searchTags(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }

        // 去除可能的 # 前缀
        String cleanKeyword = keyword.startsWith("#") ? keyword.substring(1) : keyword;

        return tagRepository.searchByName(cleanKeyword.trim())
                .stream()
                .map(TagDto::fromEntity)
                .toList();
    }

    /**
     * 获取热门标签（按关联已发布文章数量倒序）
     * @param limit 返回数量限制，null 则返回全部
     */
    @Transactional(readOnly = true)
    public List<TagWithCountDto> getHotTags(Integer limit) {
        List<Object[]> results = tagRepository.findHotTags();

        var stream = results.stream()
                .map(row -> TagWithCountDto.fromEntityAndCount(
                        (Tag) row[0],
                        ((Number) row[1]).longValue()
                ));

        if (limit != null && limit > 0) {
            stream = stream.limit(limit);
        }

        return stream.toList();
    }

    /**
     * 获取所有标签及其文章数统计
     */
    @Transactional(readOnly = true)
    public List<TagWithCountDto> getAllTagsWithCount() {
        return tagRepository.findAllWithArticleCount()
                .stream()
                .map(row -> TagWithCountDto.fromEntityAndCount(
                        (Tag) row[0],
                        ((Number) row[1]).longValue()
                ))
                .toList();
    }

    /**
     * 获取与指定标签相关的其他标签（共现标签推荐）
     * 原理：查找与该标签出现在同一篇文章中的其他标签
     * @param tagId 标签 ID
     * @param limit 返回数量限制，null 则返回全部
     */
    @Transactional(readOnly = true)
    public List<TagWithCountDto> getRelatedTags(Long tagId, Integer limit) {
        if (!tagRepository.existsById(tagId)) {
            throw new NotFoundException("标签不存在");
        }

        List<Object[]> results = tagRepository.findRelatedTags(tagId);

        var stream = results.stream()
                .map(row -> TagWithCountDto.fromEntityAndCount(
                        (Tag) row[0],
                        ((Number) row[1]).longValue()
                ));

        if (limit != null && limit > 0) {
            stream = stream.limit(limit);
        }

        return stream.toList();
    }
}
