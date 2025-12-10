package com.tiktel.ttelgo.blog.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogPostDto {
    private Long id;
    private String slug;
    private String title;
    private String excerpt;
    private String content;
    private String featuredImage;
    private String category;
    private String readTime;
    private Boolean isFeatured;
    private Boolean isPublished;
    private LocalDateTime publishedAt;
    private Long authorId;
    private String authorName;
    private String metaTitle;
    private String metaDescription;
    private String tags;
    private Long viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

