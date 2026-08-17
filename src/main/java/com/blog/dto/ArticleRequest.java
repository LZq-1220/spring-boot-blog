package com.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
public class ArticleRequest {
    @NotBlank
    @Size(max = 200)
    private String title;

    @Size(max = 1000)
    private String summary;

    @NotBlank
    private String content;

    /**
     * 只允许这两个值：原先直接 Status.valueOf() 未校验，
     * 传小写或非法值会抛 IllegalArgumentException 变成 500。
     * 显式传 null 时由 service 兜底成 DRAFT。
     */
    @Pattern(regexp = "DRAFT|PUBLISHED", message = "必须是 DRAFT 或 PUBLISHED")
    private String status = "DRAFT";

    private Long categoryId;

    private Set<Long> tagIds;
}
