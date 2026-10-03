package com.bosch.ecommerce.tax.exception;

import com.bosch.ecommerce.tax.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(TaxClassificationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleClassificationNotFound(TaxClassificationNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "TAX_CLASSIFICATION_NOT_FOUND", ex.getMessage(), req, List.of());
    }

    @ExceptionHandler(TaxRuleNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRuleNotFound(TaxRuleNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "TAX_RULE_NOT_FOUND", ex.getMessage(), req, List.of());
    }

    @ExceptionHandler(TaxRateNotVerifiedException.class)
    public ResponseEntity<ErrorResponse> handleRateNotVerified(TaxRateNotVerifiedException ex, HttpServletRequest req) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "TAX_RATE_NOT_VERIFIED", ex.getMessage(), req, List.of());
    }

    @ExceptionHandler(InvalidTaxableAmountException.class)
    public ResponseEntity<ErrorResponse> handleInvalidAmount(InvalidTaxableAmountException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_TAXABLE_AMOUNT", ex.getMessage(), req, List.of());
    }

    @ExceptionHandler(InvalidTransactionDateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidDate(InvalidTransactionDateException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_TRANSACTION_DATE", ex.getMessage(), req, List.of());
    }

    @ExceptionHandler(InvalidStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidState(InvalidStateException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_STATE", ex.getMessage(), req, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .sorted()
                .toList();
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed", req, details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST",
                "Request body is missing or malformed (check JSON syntax, numbers and yyyy-MM-dd dates)", req, List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER",
                "Invalid value for parameter '" + ex.getName() + "'. Expected format: yyyy-MM-dd", req, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest req) {
        log.error("Unexpected error on {}", req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred", req, List.of());
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String code, String message,
                                                HttpServletRequest req, List<String> details) {
        ErrorResponse body = new ErrorResponse(LocalDateTime.now(), status.value(), code, message,
                req.getRequestURI(), details);
        return ResponseEntity.status(status).body(body);
    }
}
