package com.raktim.search_service.common.web;

import java.util.List;

/** Same page format as catalogue's, copied rather than shared. */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
}