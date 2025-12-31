package com.tiktel.ttelgo.blog.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateBlogPostRequest {
    @NotBlank(message = "Slug is required")
    private String slug;
    
    @NotBlank(message = "Title is required")
    private String title;
    
    private String excerpt;
    
    @NotBlank(message = "Content is required")
    private String content;
    
    private String featuredImage;
    
    @NotBlank(message = "Category is required")
    private String category;
    
    private String readTime;
    
    private Boolean isFeatured = false;
    
    private Boolean isPublished = false;
    
    private Long authorId;
    
    private String authorName;
    
    private String metaTitle;
    
    private String metaDescription;
    
    private String tags;
}

