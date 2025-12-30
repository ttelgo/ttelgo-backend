package com.tiktel.ttelgo.blog.infrastructure.repository;

import com.tiktel.ttelgo.blog.domain.BlogPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {
    Optional<BlogPost> findBySlug(String slug);
    
    boolean existsBySlug(String slug);
    
    Page<BlogPost> findByIsPublishedTrue(Pageable pageable);
    
    Page<BlogPost> findByIsPublishedTrueAndCategory(String category, Pageable pageable);
    
    @Query("SELECT b FROM BlogPost b WHERE b.isPublished = true AND " +
           "(LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(b.excerpt) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(b.content) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<BlogPost> searchPublishedPosts(@Param("query") String query, Pageable pageable);
    
    Page<BlogPost> findByIsPublishedTrueAndIsFeaturedTrue(Pageable pageable);
    
    @Query("SELECT b FROM BlogPost b WHERE b.isPublished = true AND " +
           "b.tags LIKE CONCAT('%', :tag, '%')")
    Page<BlogPost> findByTag(@Param("tag") String tag, Pageable pageable);
}

