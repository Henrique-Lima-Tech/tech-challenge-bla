package com.challenge.aitools.taskmanagement.application.shared.pagination;

import java.util.List;
import java.util.function.Function;

public record Page<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public Page {
        content = List.copyOf(content);
    }

    public <R> Page<R> map(final Function<T, R> mapper) {
        return new Page<>(content.stream().map(mapper).toList(), page, size, totalElements, totalPages);
    }
}
