package com.bosch.ecommerce.tax.exception;

public class InvalidTransactionDateException extends RuntimeException {
    public InvalidTransactionDateException(String message) {
        super(message);
    }
}
