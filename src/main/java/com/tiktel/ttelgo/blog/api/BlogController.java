package com.tiktel.ttelgo.blog.api;

import com.tiktel.ttelgo.blog.api.dto.BlogPostDto;
import com.tiktel.ttelgo.blog.api.dto.BlogPostsResponse;
import com.tiktel.ttelgo.blog.api.dto.CreateBlogPostRequest;
import com.tiktel.ttelgo.blog.api.dto.UpdateBlogPostRequest;
import com.tiktel.ttelgo.blog.application.BlogService;
import com.tiktel.ttelgo.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
// import org.springframework.security.access.prepost.PreAuthorize; // Temporarily disabled for development
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/blog")
@RequiredArgsConstructor
public class BlogController {
    
    private final BlogService blogService;
    
    /**
     * Get all published blog posts (public endpoint)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<BlogPostsResponse>> getAllPosts(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String search
    ) {
        BlogPostsResponse response = blogService.getAllPosts(page, limit, category, tag, search);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * Get blog post by slug (public endpoint)
     */
    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<BlogPostDto>> getPostBySlug(@PathVariable String slug) {
        BlogPostDto post = blogService.getPostBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success(post));
    }
    
    /**
     * Get featured blog posts (public endpoint)
     */
    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<List<BlogPostDto>>> getFeaturedPosts(
            @RequestParam(required = false) Integer limit
    ) {
        List<BlogPostDto> posts = blogService.getFeaturedPosts(limit);
        return ResponseEntity.ok(ApiResponse.success(posts));
    }
    
    /**
     * Search blog posts (public endpoint)
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<BlogPostsResponse>> searchPosts(
            @RequestParam String q,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit
    ) {
        BlogPostsResponse response = blogService.getAllPosts(page, limit, null, null, q);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    // ========== Admin Endpoints ==========
    
    /**
     * Get all blog posts including unpublished (admin only)
     * Note: @PreAuthorize removed for development - add back in production with proper authentication
     */
    @GetMapping("/admin/all")
    // @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')") // Temporarily disabled for development
    public ResponseEntity<ApiResponse<BlogPostsResponse>> getAllPostsAdmin(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit
    ) {
        BlogPostsResponse response = blogService.getAllPostsAdmin(page, limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * Get blog post by ID (admin only)
     * Note: @PreAuthorize removed for development - add back in production with proper authentication
     */
    @GetMapping("/admin/{id}")
    // @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')") // Temporarily disabled for development
    public ResponseEntity<ApiResponse<BlogPostDto>> getPostById(@PathVariable Long id) {
        BlogPostDto post = blogService.getPostById(id);
        return ResponseEntity.ok(ApiResponse.success(post));
    }
    
    /**
     * Create new blog post (admin only)
     * Note: @PreAuthorize removed for development - add back in production with proper authentication
     */
    @PostMapping
    // @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')") // Temporarily disabled for development
    public ResponseEntity<ApiResponse<BlogPostDto>> createPost(@Valid @RequestBody CreateBlogPostRequest request) {
        BlogPostDto post = blogService.createPost(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(post));
    }
    
    /**
     * Update blog post (admin only)
     * Note: @PreAuthorize removed for development - add back in production with proper authentication
     */
    @PutMapping("/{id}")
    // @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')") // Temporarily disabled for development
    public ResponseEntity<ApiResponse<BlogPostDto>> updatePost(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBlogPostRequest request
    ) {
        BlogPostDto post = blogService.updatePost(id, request);
        return ResponseEntity.ok(ApiResponse.success(post));
    }
    
    /**
     * Delete blog post (admin only)
     * Note: @PreAuthorize removed for development - add back in production with proper authentication
     */
    @DeleteMapping("/{id}")
    // @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')") // Temporarily disabled for development
    public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable Long id) {
        blogService.deletePost(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

