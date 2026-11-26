package com.raktim.search_service.client;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CatalogueClient {

    private final RestClient catalogueRestClient;
    private final CatalogueClientProperties properties;

    /** Reads every product, one page at a time. Throws RestClientException if catalogue fails or is too slow. */
    public List<CatalogueProduct> fetchAll() {
        List<CatalogueProduct> all = new ArrayList<>();
        int page = 0;
        int totalPages;
        do {
            CataloguePage response = fetchPage(page);
            all.addAll(response.content());
            totalPages = response.totalPages();
            page++;
        } while (page < totalPages);
        return all;
    }

    private CataloguePage fetchPage(int page) {
        CataloguePage response = catalogueRestClient.get()
                .uri(uri -> uri.path("/api/v1/products")
                        .queryParam("page", page)
                        .queryParam("size", properties.pageSize())
                        .queryParam("sort", "id")               // stable order, so pages don't overlap
                        .build())
                .retrieve()                                     // 4xx/5xx become exceptions
                .body(CataloguePage.class);
        if (response == null) {
            throw new RestClientException("catalogue returned an empty body for page " + page);
        }
        return response;
    }
}