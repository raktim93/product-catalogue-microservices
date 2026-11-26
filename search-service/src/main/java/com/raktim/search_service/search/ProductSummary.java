package com.raktim.search_service.search;

import java.math.BigDecimal;

/** One search hit: the same four fields as in Stage 2, so clients see no change. */
public record ProductSummary(long id, String name, String category, BigDecimal price) {
}