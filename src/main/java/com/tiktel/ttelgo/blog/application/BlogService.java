package com.tiktel.ttelgo.blog.application;

import com.tiktel.ttelgo.blog.api.dto.BlogPostDto;
import com.tiktel.ttelgo.blog.api.dto.BlogPostsResponse;
import com.tiktel.ttelgo.blog.api.dto.CreateBlogPostRequest;
import com.tiktel.ttelgo.blog.api.dto.UpdateBlogPostRequest;
import com.tiktel.ttelgo.blog.api.mapper.BlogApiMapper;
import com.tiktel.ttelgo.blog.domain.BlogPost;
import com.tiktel.ttelgo.blog.infrastructure.repository.BlogPostRepository;
import com.tiktel.ttelgo.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BlogService {
    
    private final BlogPostRepository blogPostRepository;
    
    @Transactional(readOnly = true)
    public BlogPostsResponse getAllPosts(Integer page, Integer limit, String category, String tag, String search) {
        Pageable pageable = PageRequest.of(
                page != null ? page : 0,
                limit != null ? limit : 10,
                Sort.by(Sort.Direction.DESC, "publishedAt", "createdAt")
        );
        
        Page<BlogPost> blogPostPage;
        
        if (search != null && !search.trim().isEmpty()) {
            blogPostPage = blogPostRepository.searchPublishedPosts(search.trim(), pageable);
        } else if (tag != null && !tag.trim().isEmpty()) {
            blogPostPage = blogPostRepository.findByTag(tag.trim(), pageable);
        } else if (category != null && !category.trim().isEmpty()) {
            blogPostPage = blogPostRepository.findByIsPublishedTrueAndCategory(category.trim(), pageable);
        } else {
            blogPostPage = blogPostRepository.findByIsPublishedTrue(pageable);
        }
        
        List<BlogPostDto> posts = BlogApiMapper.toDtoList(blogPostPage.getContent());
        
        return BlogPostsResponse.builder()
                .posts(posts)
                .total(blogPostPage.getTotalElements())
                .page(blogPostPage.getNumber())
                .totalPages(blogPostPage.getTotalPages())
                .limit(blogPostPage.getSize())
                .build();
    }
    
    @Transactional(readOnly = true)
    public BlogPostDto getPostBySlug(String slug) {
        BlogPost blogPost = blogPostRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Blog post not found with slug: " + slug));
        
        // Increment view count
        blogPost.setViewCount(blogPost.getViewCount() + 1);
        blogPostRepository.save(blogPost);
        
        return BlogApiMapper.toDto(blogPost);
    }
    
    @Transactional(readOnly = true)
    public List<BlogPostDto> getFeaturedPosts(Integer limit) {
        Pageable pageable = PageRequest.of(0, limit != null ? limit : 3);
        Page<BlogPost> featuredPosts = blogPostRepository.findByIsPublishedTrueAndIsFeaturedTrue(pageable);
        return BlogApiMapper.toDtoList(featuredPosts.getContent());
    }
    
    @Transactional
    public BlogPostDto createPost(CreateBlogPostRequest request) {
        // Check if slug already exists
        if (blogPostRepository.existsBySlug(request.getSlug())) {
            throw new IllegalArgumentException("Blog post with slug '" + request.getSlug() + "' already exists");
        }
        
        BlogPost blogPost = BlogApiMapper.toEntity(request);
        BlogPost savedPost = blogPostRepository.save(blogPost);
        return BlogApiMapper.toDto(savedPost);
    }
    
    @Transactional
    public BlogPostDto updatePost(Long id, UpdateBlogPostRequest request) {
        BlogPost blogPost = blogPostRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blog post not found with id: " + id));
        
        // Check if slug is being updated and if it conflicts with another post
        if (request.getSlug() != null && !request.getSlug().equals(blogPost.getSlug())) {
            if (blogPostRepository.existsBySlug(request.getSlug())) {
                throw new IllegalArgumentException("Blog post with slug '" + request.getSlug() + "' already exists");
            }
        }
        
        BlogApiMapper.updateEntity(blogPost, request);
        BlogPost updatedPost = blogPostRepository.save(blogPost);
        return BlogApiMapper.toDto(updatedPost);
    }
    
    @Transactional
    public void deletePost(Long id) {
        if (!blogPostRepository.existsById(id)) {
            throw new ResourceNotFoundException("Blog post not found with id: " + id);
        }
        blogPostRepository.deleteById(id);
    }
    
    // Admin methods - get all posts including unpublished
    @Transactional(readOnly = true)
    public BlogPostsResponse getAllPostsAdmin(Integer page, Integer limit) {
        Pageable pageable = PageRequest.of(
                page != null ? page : 0,
                limit != null ? limit : 10,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        
        Page<BlogPost> blogPostPage = blogPostRepository.findAll(pageable);
        
        List<BlogPostDto> posts = BlogApiMapper.toDtoList(blogPostPage.getContent());
        
        return BlogPostsResponse.builder()
                .posts(posts)
                .total(blogPostPage.getTotalElements())
                .page(blogPostPage.getNumber())
                .totalPages(blogPostPage.getTotalPages())
                .limit(blogPostPage.getSize())
                .build();
    }
    
    @Transactional(readOnly = true)
    public BlogPostDto getPostById(Long id) {
        BlogPost blogPost = blogPostRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blog post not found with id: " + id));
        return BlogApiMapper.toDto(blogPost);
    }
}

