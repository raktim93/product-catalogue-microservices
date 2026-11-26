package com.raktim.catalogue_service.catalogue.web;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** What a client may send to create a product. No id, no version: the server owns those. */
public record CreateProductRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 2000) String description,
        @NotBlank @Size(max = 50) String category,
        @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal price) {
}