package com.raktim.catalogue_service.catalogue.web;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Full replacement of the editable fields, plus the version the client last read (CP 2.6). */
public record UpdateProductRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 2000) String description,
        @NotBlank @Size(max = 50) String category,
        @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal price,
        @NotNull Long version) {
}