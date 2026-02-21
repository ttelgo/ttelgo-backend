package com.tiktel.ttelgo.blog.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "blog_posts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogPost {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String slug;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String excerpt;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "featured_image")
    private String featuredImage;
    
    @Column(nullable = false)
    private String category;
    
    @Column(name = "read_time")
    private String readTime;
    
    @Column(name = "is_featured")
    @Builder.Default
    private Boolean isFeatured = false;
    
    @Column(name = "is_published")
    @Builder.Default
    private Boolean isPublished = false;
    
    @Column(name = "published_at")
    private LocalDateTime publishedAt;
    
    @Column(name = "author_id")
    private Long authorId;
    
    @Column(name = "author_name")
    private String authorName;
    
    @Column(name = "meta_title")
    private String metaTitle;
    
    @Column(name = "meta_description", columnDefinition = "TEXT")
    private String metaDescription;
    
    @Column(name = "tags")
    private String tags; // Comma-separated tags
    
    @Column(name = "view_count")
    @Builder.Default
    private Long viewCount = 0L;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isPublished && publishedAt == null) {
            publishedAt = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (isPublished && publishedAt == null) {
            publishedAt = LocalDateTime.now();
        }
    }
}

