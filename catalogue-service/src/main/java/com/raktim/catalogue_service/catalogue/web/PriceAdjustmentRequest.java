package com.raktim.catalogue_service.catalogue.web;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PriceAdjustmentRequest(
        @NotEmpty @Size(max = 50) @Valid List<Item> adjustments) {   // @Valid: also validate every Item

    public record Item(
            // a nested record is static automatically
            @NotNull Long productId,
            @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal newPrice) {
    }
}