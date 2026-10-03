package com.bosch.ecommerce.price.exception;

/** Version/validity conflict (e.g. concurrent creation or non-increasing validFrom). Mapped to HTTP 409. */
public class PriceConflictException extends RuntimeException {
    public PriceConflictException(String message) {
        super(message);
    }
}
