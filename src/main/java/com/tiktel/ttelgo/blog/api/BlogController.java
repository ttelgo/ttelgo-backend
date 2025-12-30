package com.tiktel.ttelgo.blog.api;

import com.tiktel.ttelgo.blog.api.dto.BlogPostDto;
import com.tiktel.ttelgo.blog.api.dto.BlogPostsResponse;
import com.tiktel.ttelgo.blog.application.BlogService;
import com.tiktel.ttelgo.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
// import org.springframework.security.access.prepost.PreAuthorize; // Temporarily disabled for development
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/posts")
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
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean featured
    ) {
        if (Boolean.TRUE.equals(featured)) {
            BlogPostsResponse response = BlogPostsResponse.builder()
                    .posts(blogService.getFeaturedPosts(limit))
                    .build();
            return ResponseEntity.ok(ApiResponse.success(response, "Success", null));
        }

        BlogPostsResponse response = blogService.getAllPosts(page, limit, category, tag, q);
        return ResponseEntity.ok(ApiResponse.success(response, "Success", toPaginationMeta(response)));
    }
    
    /**
     * Get blog post by slug (public endpoint)
     */
    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<BlogPostDto>> getPostBySlug(@PathVariable String slug) {
        BlogPostDto post = blogService.getPostBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success(post));
    }

    private Map<String, Object> toPaginationMeta(BlogPostsResponse response) {
        if (response == null || response.getTotal() == null) return null;
        Map<String, Object> pagination = new LinkedHashMap<>();
        pagination.put("page", response.getPage());
        pagination.put("size", response.getLimit());
        pagination.put("totalElements", response.getTotal());
        pagination.put("totalPages", response.getTotalPages());

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("pagination", pagination);
        return meta;
    }
}

