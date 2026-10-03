package com.bosch.ecommerce.tax.exception;

public class TaxRateNotVerifiedException extends RuntimeException {
    public TaxRateNotVerifiedException(String message) {
        super(message);
    }
}
