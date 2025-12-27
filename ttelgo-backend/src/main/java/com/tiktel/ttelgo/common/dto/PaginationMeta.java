package com.tiktel.ttelgo.common.dto;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class PaginationMeta {
    private PaginationMeta() {}

    public static Map<String, Object> fromPage(Page<?> page) {
        Map<String, Object> pagination = new LinkedHashMap<>();
        pagination.put("page", page.getNumber());
        pagination.put("size", page.getSize());
        pagination.put("totalElements", page.getTotalElements());
        pagination.put("totalPages", page.getTotalPages());
        pagination.put("numberOfElements", page.getNumberOfElements());
        pagination.put("first", page.isFirst());
        pagination.put("last", page.isLast());

        Sort sort = page.getSort();
        if (sort != null && sort.isSorted()) {
            pagination.put("sort", sort.stream().map(o -> o.getProperty() + "," + o.getDirection().name().toLowerCase())
                    .collect(Collectors.toList()));
        }

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("pagination", pagination);
        return meta;
    }

    public static Map<String, Object> simple(int page, int size, long totalElements) {
        int totalPages = size <= 0 ? 1 : (int) Math.ceil((double) totalElements / (double) size);
        Map<String, Object> pagination = new LinkedHashMap<>();
        pagination.put("page", page);
        pagination.put("size", size);
        pagination.put("totalElements", totalElements);
        pagination.put("totalPages", totalPages);
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("pagination", pagination);
        return meta;
    }
}


