package com.bosch.ecommerce.tax.dto;

import java.time.LocalDate;

public record TaxClassificationResponse(
        String productId,
        String hsnCode,
        String hsnLevel,
        String hsnDescription,
        String verificationStatus,
        String taxSource,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        String status) {
}
