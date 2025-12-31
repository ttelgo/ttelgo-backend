package com.tiktel.ttelgo.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResponse<T> {
    private List<T> content;
    private PageMetadata meta;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageMetadata {
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean first;
        private boolean last;
        private boolean empty;
        private String sortBy;
        private String sortDirection;
    }
    
    public static <T> PageResponse<T> of(List<T> content, org.springframework.data.domain.Page<?> page) {
        PageMetadata meta = PageMetadata.builder()
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .empty(page.isEmpty())
                .build();
        
        if (page.getSort().isSorted()) {
            page.getSort().forEach(order -> {
                meta.setSortBy(order.getProperty());
                meta.setSortDirection(order.getDirection().name());
            });
        }
        
        return PageResponse.<T>builder()
                .content(content)
                .meta(meta)
                .build();
    }
}

