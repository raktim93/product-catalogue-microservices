package com.raktim.search_service.search;

import java.math.BigDecimal;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.raktim.search_service.common.web.PageResponse;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductSearchController {

    private final ProductSearchService productSearch;

    @GetMapping("/search")
    public ResponseEntity<PageResponse<ProductSummary>> search(@RequestParam(required = false) String q,
                                                               @RequestParam(required = false) String category,
                                                               @RequestParam(required = false) BigDecimal minPrice,
                                                               @RequestParam(required = false) BigDecimal maxPrice,
                                                               @RequestParam(defaultValue = "0") @Min(0) int page,
                                                               @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size,
                                                               @RequestParam(defaultValue = "relevance") String sort) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minPrice must not be greater than maxPrice");
        }
        long started = System.nanoTime();
        PageResponse<ProductSummary> result =
                productSearch.search(new ProductSearchCriteria(q, category, minPrice, maxPrice), page, size, sort);
        String tookMs = String.format(Locale.ROOT, "%.2f", (System.nanoTime() - started) / 1_000_000.0);
        log.info("[search] q={} category={} page={} -> {} hits in {} ms",
                q, category, page, result.totalElements(), tookMs);
        return ResponseEntity.ok()
                .header("X-Search-Took-Ms", tookMs)                     // CAT-67: time inside search-service
                .body(result);
    }
}