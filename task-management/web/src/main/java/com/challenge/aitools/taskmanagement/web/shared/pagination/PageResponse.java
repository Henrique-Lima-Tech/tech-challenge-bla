package com.challenge.aitools.taskmanagement.web.shared.pagination;

import java.util.List;

import com.challenge.aitools.taskmanagement.application.shared.pagination.Page;

public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> from(final Page<T> page) {
        return new PageResponse<>(page.content(), page.page(), page.size(), page.totalElements(), page.totalPages());
    }
}
