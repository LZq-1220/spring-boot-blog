package com.blog.config;

import com.blog.entity.Category;
import com.blog.entity.Tag;
import com.blog.repository.CategoryRepository;
import com.blog.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

/**
 * 开发用种子数据。
 * 项目没有分类/标签的管理接口，库里为空时侧栏什么都不显示、
 * 文章也无法归类，所以在 h2 profile 下预置几条。
 * 只在表为空时写入，重复启动不会累积。
 */
@Configuration
@Profile("h2")
@RequiredArgsConstructor
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public ApplicationRunner seedMetadata(CategoryRepository categoryRepository,
                                          TagRepository tagRepository) {
        return args -> {
            if (categoryRepository.count() == 0) {
                List.of("技术笔记", "读书随想", "生活记录").forEach(name ->
                        categoryRepository.save(Category.builder().name(name).build()));
                log.info("已写入默认分类");
            }
            if (tagRepository.count() == 0) {
                List.of(
                        "Java", "Spring", "Spring Boot", "前端", "数据库", "杂谈",
                        "Python", "Go", "JavaScript", "TypeScript", "C++",
                        "MySQL", "Redis", "Docker", "Kubernetes", "Linux",
                        "算法", "数据结构", "网络", "操作系统", "分布式系统",
                        "微服务", "设计模式", "面试", "开源"
                ).forEach(name ->
                        tagRepository.save(Tag.builder().name(name).build()));
                log.info("已写入默认标签");
            }
        };
    }
}
