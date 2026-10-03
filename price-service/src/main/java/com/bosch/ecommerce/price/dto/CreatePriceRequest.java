package com.bosch.ecommerce.price.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/** API contract for creating a new price version. The client can NOT choose the version. */
public record CreatePriceRequest(

        @Schema(example = "BOS-001")
        @NotBlank(message = "productId is required")
        @Size(max = 50, message = "productId must be at most 50 characters")
        String productId,

        @Schema(example = "INR")
        @NotBlank(message = "currency is required")
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "currency must be a 3-letter ISO code, e.g. INR")
        String currency,

        @Schema(example = "7999.00")
        @NotNull(message = "basePrice is required")
        @DecimalMin(value = "0.00", message = "basePrice must be >= 0")
        @Digits(integer = 10, fraction = 2, message = "basePrice allows at most 10 integer and 2 decimal digits")
        BigDecimal basePrice,

        @Schema(example = "7199.10")
        @NotNull(message = "sellingPrice is required")
        @DecimalMin(value = "0.00", message = "sellingPrice must be >= 0")
        @Digits(integer = 10, fraction = 2, message = "sellingPrice allows at most 10 integer and 2 decimal digits")
        BigDecimal sellingPrice,

        @Schema(example = "10.00")
        @NotNull(message = "discountPercent is required")
        @DecimalMin(value = "0.00", message = "discountPercent must be >= 0")
        @DecimalMax(value = "100.00", message = "discountPercent must be <= 100")
        @Digits(integer = 3, fraction = 2, message = "discountPercent allows at most 2 decimal digits")
        BigDecimal discountPercent,

        @Schema(example = "2026-10-03")
        @NotNull(message = "validFrom is required")
        LocalDate validFrom) {
}
