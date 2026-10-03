package com.bosch.ecommerce.price.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Standard error body returned for every failure. */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<String> details) {
}
