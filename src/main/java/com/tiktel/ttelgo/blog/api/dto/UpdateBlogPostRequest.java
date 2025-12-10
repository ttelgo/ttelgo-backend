package com.tiktel.ttelgo.blog.api.dto;

import lombok.Data;

@Data
public class UpdateBlogPostRequest {
    private String slug;
    private String title;
    private String excerpt;
    private String content;
    private String featuredImage;
    private String category;
    private String readTime;
    private Boolean isFeatured;
    private Boolean isPublished;
    private String authorName;
    private String metaTitle;
    private String metaDescription;
    private String tags;
}

