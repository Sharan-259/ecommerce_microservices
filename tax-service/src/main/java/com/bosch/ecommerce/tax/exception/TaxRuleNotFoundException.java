package com.bosch.ecommerce.tax.exception;

public class TaxRuleNotFoundException extends RuntimeException {
    public TaxRuleNotFoundException(String message) {
        super(message);
    }
}
