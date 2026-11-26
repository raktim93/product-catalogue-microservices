package com.raktim.catalogue_service.catalogue.service;

import java.math.BigDecimal;

/**
 * One requested price change, as the service sees it (independent of the web DTO).
 */
public record PriceChange(long productId, BigDecimal newPrice) {

}