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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Admin blog management as a RESTful resource.
 */
@RestController
@RequestMapping("/api/v1/admin/posts")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminPostController {

    private final BlogService blogService;

    @GetMapping
    public ResponseEntity<ApiResponse<BlogPostsResponse>> listAllPosts(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit
    ) {
        BlogPostsResponse response = blogService.getAllPostsAdmin(page, limit);
        return ResponseEntity.ok(ApiResponse.success(response, "Success", toPaginationMeta(response)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BlogPostDto>> getPost(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(blogService.getPostById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BlogPostDto>> createPost(@Valid @RequestBody CreateBlogPostRequest request) {
        BlogPostDto post = blogService.createPost(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(post));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BlogPostDto>> updatePost(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBlogPostRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(blogService.updatePost(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> deletePost(@PathVariable Long id) {
        blogService.deletePost(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Deleted", null));
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


