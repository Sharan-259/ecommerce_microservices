package com.bosch.ecommerce.price.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** One row of the price history (one version of a product's price). */
public record PriceHistoryResponse(
        Long priceId,
        String productId,
        String currency,
        BigDecimal basePrice,
        BigDecimal sellingPrice,
        BigDecimal discountPercent,
        Integer priceVersion,
        LocalDate validFrom,
        LocalDate validTo,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
