package com.raktim.catalogue_service.catalogue.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.raktim.catalogue_service.catalogue.domain.Product;

/** What catalogue tells other services after a product changes. Sent as JSON, keyed by productId. */
public record ProductEvent(UUID eventId, String type, long productId, long version,
                           Instant occurredAt, ProductData product) {

    public static final String CREATED = "ProductCreated";
    public static final String UPDATED = "ProductUpdated";
    public static final String DELETED = "ProductDeleted";

    /** The product after the change. Null for a deleted. */
    public record ProductData(long id, String name, String description, String category, BigDecimal price) {
    }

    public static ProductEvent created(Product product) {
        return withData(CREATED, product);
    }

    public static ProductEvent updated(Product product) {
        return withData(UPDATED, product);
    }

    /** A deleted is a change too, so it gets the next version. */
    public static ProductEvent deleted(Product product) {
        return new ProductEvent(UUID.randomUUID(), DELETED, product.getId(), product.getVersion() + 1,
                Instant.now(), null);
    }

    private static ProductEvent withData(String type, Product product) {
        ProductData data = new ProductData(product.getId(), product.getName(), product.getDescription(),
                product.getCategory(), product.getPrice());
        return new ProductEvent(UUID.randomUUID(), type, product.getId(), product.getVersion(),
                Instant.now(), data);
    }
}