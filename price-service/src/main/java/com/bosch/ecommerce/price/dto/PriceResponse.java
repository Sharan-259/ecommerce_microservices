package com.bosch.ecommerce.price.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PriceResponse(
        String productId,
        String currency,
        BigDecimal basePrice,
        BigDecimal sellingPrice,
        BigDecimal discountPercent,
        Integer priceVersion,
        LocalDate validFrom,
        LocalDate validTo,
        String status) {
}
