package com.raktim.search_service.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** The event as search reads it. A field catalogue adds later is ignored, not an error. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductEvent(UUID eventId, String type, long productId, long version,
                           Instant occurredAt, ProductData product) {

    public static final String CREATED = "ProductCreated";
    public static final String UPDATED = "ProductUpdated";
    public static final String DELETED = "ProductDeleted";

    public ProductEvent {
        if (eventId == null || type == null) {
            throw new IllegalArgumentException("eventId and type are required");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProductData(long id, String name, String description, String category, BigDecimal price) {
    }
}