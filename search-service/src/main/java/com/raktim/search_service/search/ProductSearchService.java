package com.raktim.search_service.search;

import org.springframework.stereotype.Service;

import com.raktim.search_service.common.web.PageResponse;
import com.raktim.search_service.projection.ProductStore;

import lombok.RequiredArgsConstructor;

/** Runs a search on search's copy of the products. */
@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private final ProductStore store;

    public PageResponse<ProductSummary> search(ProductSearchCriteria criteria, int page, int size, String sort) {
        return store.search(criteria, page, size, sort);
    }
}