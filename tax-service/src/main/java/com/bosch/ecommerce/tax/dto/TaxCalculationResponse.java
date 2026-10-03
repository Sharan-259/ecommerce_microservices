package com.bosch.ecommerce.tax.dto;

import java.math.BigDecimal;
import java.util.List;

public record TaxCalculationResponse(
        String productId,
        BigDecimal taxableAmount,
        String hsnCode,
        String taxType,
        BigDecimal cgstRate,
        BigDecimal sgstRate,
        BigDecimal igstRate,
        BigDecimal cessRate,
        BigDecimal cgstAmount,
        BigDecimal sgstAmount,
        BigDecimal igstAmount,
        BigDecimal cessAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        String taxRuleId,
        String verificationStatus,
        List<String> warnings) {
}
