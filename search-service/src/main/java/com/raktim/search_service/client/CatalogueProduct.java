package com.raktim.search_service.client;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** One product as catalogue's JSON has it. */
@JsonIgnoreProperties(ignoreUnknown = true)   // a field catalogue adds later can't break us
public record CatalogueProduct(long id, String name, String description, String category,
                               BigDecimal price, long version) {
}