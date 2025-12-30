package com.tiktel.ttelgo.blog.api.mapper;

import com.tiktel.ttelgo.blog.api.dto.BlogPostDto;
import com.tiktel.ttelgo.blog.api.dto.CreateBlogPostRequest;
import com.tiktel.ttelgo.blog.api.dto.UpdateBlogPostRequest;
import com.tiktel.ttelgo.blog.domain.BlogPost;

import java.util.List;
import java.util.stream.Collectors;

public class BlogApiMapper {
    
    public static BlogPostDto toDto(BlogPost blogPost) {
        if (blogPost == null) {
            return null;
        }
        
        return BlogPostDto.builder()
                .id(blogPost.getId())
                .slug(blogPost.getSlug())
                .title(blogPost.getTitle())
                .excerpt(blogPost.getExcerpt())
                .content(blogPost.getContent())
                .featuredImage(blogPost.getFeaturedImage())
                .category(blogPost.getCategory())
                .readTime(blogPost.getReadTime())
                .isFeatured(blogPost.getIsFeatured())
                .isPublished(blogPost.getIsPublished())
                .publishedAt(blogPost.getPublishedAt())
                .authorId(blogPost.getAuthorId())
                .authorName(blogPost.getAuthorName())
                .metaTitle(blogPost.getMetaTitle())
                .metaDescription(blogPost.getMetaDescription())
                .tags(blogPost.getTags())
                .viewCount(blogPost.getViewCount())
                .createdAt(blogPost.getCreatedAt())
                .updatedAt(blogPost.getUpdatedAt())
                .build();
    }
    
    public static BlogPost toEntity(CreateBlogPostRequest request) {
        if (request == null) {
            return null;
        }
        
        return BlogPost.builder()
                .slug(request.getSlug())
                .title(request.getTitle())
                .excerpt(request.getExcerpt())
                .content(request.getContent())
                .featuredImage(request.getFeaturedImage())
                .category(request.getCategory())
                .readTime(request.getReadTime())
                .isFeatured(request.getIsFeatured() != null ? request.getIsFeatured() : false)
                .isPublished(request.getIsPublished() != null ? request.getIsPublished() : false)
                .authorId(request.getAuthorId())
                .authorName(request.getAuthorName())
                .metaTitle(request.getMetaTitle())
                .metaDescription(request.getMetaDescription())
                .tags(request.getTags())
                .viewCount(0L)
                .build();
    }
    
    public static void updateEntity(BlogPost blogPost, UpdateBlogPostRequest request) {
        if (request == null || blogPost == null) {
            return;
        }
        
        if (request.getSlug() != null) {
            blogPost.setSlug(request.getSlug());
        }
        if (request.getTitle() != null) {
            blogPost.setTitle(request.getTitle());
        }
        if (request.getExcerpt() != null) {
            blogPost.setExcerpt(request.getExcerpt());
        }
        if (request.getContent() != null) {
            blogPost.setContent(request.getContent());
        }
        if (request.getFeaturedImage() != null) {
            blogPost.setFeaturedImage(request.getFeaturedImage());
        }
        if (request.getCategory() != null) {
            blogPost.setCategory(request.getCategory());
        }
        if (request.getReadTime() != null) {
            blogPost.setReadTime(request.getReadTime());
        }
        if (request.getIsFeatured() != null) {
            blogPost.setIsFeatured(request.getIsFeatured());
        }
        if (request.getIsPublished() != null) {
            blogPost.setIsPublished(request.getIsPublished());
        }
        if (request.getAuthorName() != null) {
            blogPost.setAuthorName(request.getAuthorName());
        }
        if (request.getMetaTitle() != null) {
            blogPost.setMetaTitle(request.getMetaTitle());
        }
        if (request.getMetaDescription() != null) {
            blogPost.setMetaDescription(request.getMetaDescription());
        }
        if (request.getTags() != null) {
            blogPost.setTags(request.getTags());
        }
    }
    
    public static List<BlogPostDto> toDtoList(List<BlogPost> blogPosts) {
        if (blogPosts == null) {
            return List.of();
        }
        return blogPosts.stream()
                .map(BlogApiMapper::toDto)
                .collect(Collectors.toList());
    }
}

