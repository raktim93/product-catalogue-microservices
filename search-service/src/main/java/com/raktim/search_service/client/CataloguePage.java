package com.raktim.search_service.client;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** The parts of catalogue's page JSON we use. size and totalElements are ignored. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CataloguePage(List<CatalogueProduct> content, int page, int totalPages) {
}