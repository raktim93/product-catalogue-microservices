package com.raktim.catalogue_service.catalogue.web;

import java.math.BigDecimal;

import com.raktim.catalogue_service.catalogue.domain.Product;

/** What the API returns for a product. Built from the entity, never the entity itself. */
public record ProductResponse(Long id, String name, String description, String category,
                              BigDecimal price, Long version) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getDescription(),
                product.getCategory(), product.getPrice(), product.getVersion());
    }
}