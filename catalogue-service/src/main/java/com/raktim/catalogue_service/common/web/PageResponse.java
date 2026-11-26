package com.raktim.catalogue_service.common.web;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Our own page format, so the API contract doesn't depend on how Spring Data serialises Page.
 */

public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
