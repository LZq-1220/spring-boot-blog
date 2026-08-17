package com.blog.controller;

import com.blog.dto.CategoryDto;
import com.blog.dto.TagDto;
import com.blog.repository.CategoryRepository;
import com.blog.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MetadataController {

    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;

    @GetMapping("/categories")
    public List<CategoryDto> getCategories() {
        return categoryRepository.findAll().stream().map(CategoryDto::fromEntity).toList();
    }

    @GetMapping("/tags")
    public List<TagDto> getTags() {
        return tagRepository.findAll().stream().map(TagDto::fromEntity).toList();
    }
}
