package com.tech.challenge.application.shared.pagination;

import java.util.List;

/**
 * One page of results. {@code page} is 0-based.
 */
public record PageResult<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public PageResult {
        if (page < 0) {
            throw new IllegalArgumentException("Page must not be negative");
        }
        if (size < 1) {
            throw new IllegalArgumentException("Page size must be positive");
        }
        if (totalElements < 0) {
            throw new IllegalArgumentException("Total elements must not be negative");
        }
        if (totalPages != totalPages(totalElements, size)) {
            throw new IllegalArgumentException("Total pages must match total elements and page size");
        }
        content = content == null ? List.of() : List.copyOf(content);
    }

    public static <T> PageResult<T> of(final List<T> content, final int page, final int size, final long totalElements) {
        if (size < 1) {
            throw new IllegalArgumentException("Page size must be positive");
        }
        return new PageResult<>(content, page, size, totalElements, totalPages(totalElements, size));
    }

    private static int totalPages(final long totalElements, final int size) {
        return (int) ((totalElements + size - 1) / size);
    }
}
