package com.bosch.ecommerce.tax.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Tax Service receives the taxable amount from the caller (e.g. a future Order/Checkout service).
 * It never asks Price Service for it - that is what keeps the two services decoupled.
 */
public record TaxCalculationRequest(

        @Schema(example = "BOS-001")
        @NotBlank(message = "productId is required")
        @Size(max = 50, message = "productId must be at most 50 characters")
        String productId,

        @Schema(example = "6999.00")
        @NotNull(message = "taxableAmount is required")
        BigDecimal taxableAmount,

        @Schema(example = "KA", description = "2-letter GST state/UT code of the seller")
        @NotBlank(message = "sellerState is required")
        String sellerState,

        @Schema(example = "MH", description = "2-letter GST state/UT code of the buyer")
        @NotBlank(message = "buyerState is required")
        String buyerState,

        @Schema(example = "2026-10-03")
        @NotNull(message = "transactionDate is required")
        LocalDate transactionDate) {
}
