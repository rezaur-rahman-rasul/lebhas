package com.lebhas.creativesaas.common.api;

import org.springframework.data.domain.Page;

import java.util.List;

public record PagedResult<T>(
        List<T> items,
        long totalItems,
        int totalPages,
        int page,
        int size,
        boolean first,
        boolean last
) {
    public static <T> PagedResult<T> from(Page<T> page) {
        return new PagedResult<>(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize(),
                page.isFirst(),
                page.isLast());
    }
}
