package com.tiktel.ttelgo.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Sort;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageRequest {
    @Builder.Default
    private int page = 0;
    
    @Builder.Default
    private int size = 20;
    
    private String sortBy;
    
    @Builder.Default
    private String sortDirection = "ASC";
    
    public org.springframework.data.domain.PageRequest toPageable() {
        if (sortBy != null && !sortBy.isEmpty()) {
            Sort.Direction direction = Sort.Direction.fromString(sortDirection);
            return org.springframework.data.domain.PageRequest.of(page, size, Sort.by(direction, sortBy));
        }
        return org.springframework.data.domain.PageRequest.of(page, size);
    }
    
    public org.springframework.data.domain.PageRequest toPageable(String defaultSortBy) {
        String sortField = (sortBy != null && !sortBy.isEmpty()) ? sortBy : defaultSortBy;
        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        return org.springframework.data.domain.PageRequest.of(page, size, Sort.by(direction, sortField));
    }
}

