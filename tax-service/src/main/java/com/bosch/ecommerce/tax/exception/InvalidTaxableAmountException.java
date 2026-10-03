package com.bosch.ecommerce.tax.exception;

public class InvalidTaxableAmountException extends RuntimeException {
    public InvalidTaxableAmountException(String message) {
        super(message);
    }
}
