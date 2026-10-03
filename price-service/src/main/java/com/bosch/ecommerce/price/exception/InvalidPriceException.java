package com.bosch.ecommerce.price.exception;

/** Business-rule violation (e.g. selling price above base price). Mapped to HTTP 422. */
public class InvalidPriceException extends RuntimeException {
    public InvalidPriceException(String message) {
        super(message);
    }
}
