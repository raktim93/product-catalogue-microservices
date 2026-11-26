package com.raktim.search_service.projection;

import com.raktim.search_service.common.web.PageResponse;
import com.raktim.search_service.search.ProductSearchCriteria;
import com.raktim.search_service.search.ProductSummary;

/** Search's copy of the products. ElasticsearchProductStore is the implementation (CAT-72: the only one left). */
public interface ProductStore {

    /** True if the store held no products when search started: the startup reload then fills it. */
    boolean startedEmpty();

    /** Created or updated. False if this version or a newer one is already stored. */
    boolean apply(ProductDocument document);

    /** Deleted. False if this version or a newer one is already stored. */
    boolean remove(long id, long version);

    /** Products that are not deleted. */
    long count();

    /** One page of results. An unknown sort is a 400. */
    PageResponse<ProductSummary> search(ProductSearchCriteria criteria, int page, int size, String sort);
}