package com.raktim.search_service.projection;

import java.math.BigDecimal;

import com.raktim.search_service.client.CatalogueProduct;
import com.raktim.search_service.events.ProductEvent;

/** One product in search's own copy. */
public record ProductDocument(long id, String name, String description, String category,
                              BigDecimal price, long version,boolean deleted) {

    /** From catalogue's REST API (the full reload). */
    public static ProductDocument from(CatalogueProduct product) {
        return new ProductDocument(product.id(), product.name(), product.description(),
                product.category(), product.price(), product.version(),false);
    }

    /** From a Kafka event (CAT-61). */
    public static ProductDocument from(ProductEvent event) {
        ProductEvent.ProductData product = event.product();
        return new ProductDocument(product.id(), product.name(), product.description(),
                product.category(), product.price(), event.version(),false);
    }

    /** CAT-68: "deleted at this version". No data, so it can never show up as a search result. */
    public static ProductDocument tombstone(long id, long version) {
        return new ProductDocument(id, null, null, null, null, version, true);
    }
}