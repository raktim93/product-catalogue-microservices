package com.raktim.search_service.search;

import java.math.BigDecimal;

/** Search filters. Any of them may be null, meaning "don't filter on this". */
public record ProductSearchCriteria(String text, String category, BigDecimal minPrice, BigDecimal maxPrice) {
}