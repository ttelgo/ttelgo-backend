package com.tiktel.ttelgo.blog.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogPostsResponse {
    private List<BlogPostDto> posts;
    private Long total;
    private Integer page;
    private Integer totalPages;
    private Integer limit;
}

