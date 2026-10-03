package com.bosch.ecommerce.tax.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TaxRuleResponse(
        String taxRuleId,
        String hsnCode,
        String hsnLevel,
        String hsnDescription,
        BigDecimal cgstRate,
        BigDecimal sgstRate,
        BigDecimal igstRate,
        BigDecimal cessRate,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        String status,
        String sourceReference) {
}
