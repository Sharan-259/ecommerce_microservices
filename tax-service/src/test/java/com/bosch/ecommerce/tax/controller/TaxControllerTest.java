package com.bosch.ecommerce.tax.controller;

import com.bosch.ecommerce.tax.dto.TaxCalculationResponse;
import com.bosch.ecommerce.tax.dto.TaxClassificationResponse;
import com.bosch.ecommerce.tax.dto.TaxRuleResponse;
import com.bosch.ecommerce.tax.exception.GlobalExceptionHandler;
import com.bosch.ecommerce.tax.exception.InvalidStateException;
import com.bosch.ecommerce.tax.exception.TaxClassificationNotFoundException;
import com.bosch.ecommerce.tax.exception.TaxRateNotVerifiedException;
import com.bosch.ecommerce.tax.service.TaxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Controller tests using a standalone MockMvc (no Spring Boot test slices needed). */
@ExtendWith(MockitoExtension.class)
class TaxControllerTest {

    private static final String VALID_BODY = """
            {"productId":"BOS-001","taxableAmount":10800.00,"sellerState":"KA","buyerState":"MH",
             "transactionDate":"2026-10-03"}
            """;

    @Mock
    private TaxService taxService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TaxController(taxService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private TaxCalculationResponse igstResponse() {
        BigDecimal zero = new BigDecimal("0.00");
        return new TaxCalculationResponse("BOS-001", new BigDecimal("10800.00"), "8467", "IGST",
                zero, zero, new BigDecimal("18.00"), zero,
                zero, zero, new BigDecimal("1944.00"), zero,
                new BigDecimal("1944.00"), new BigDecimal("12744.00"),
                "TR-8467", "HEADING/RATE VERIFIED; EXACT SUBHEADING CHECK", List.of("demo"));
    }

    @Test
    void calculate_validRequest_returns200() throws Exception {
        when(taxService.calculate(any())).thenReturn(igstResponse());

        mockMvc.perform(post("/api/v1/taxes/calculate").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value("BOS-001"))
                .andExpect(jsonPath("$.taxType").value("IGST"))
                .andExpect(jsonPath("$.taxAmount").value(1944.00))
                .andExpect(jsonPath("$.totalAmount").value(12744.00));
    }

    @Test
    void calculate_missingFields_returns400WithValidationDetails() throws Exception {
        mockMvc.perform(post("/api/v1/taxes/calculate").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details").isArray());

        verify(taxService, never()).calculate(any());
    }

    @Test
    void calculate_malformedDate_returns400() throws Exception {
        String body = """
                {"productId":"BOS-001","taxableAmount":10800.00,"sellerState":"KA","buyerState":"MH",
                 "transactionDate":"03/10/2026"}
                """;

        mockMvc.perform(post("/api/v1/taxes/calculate").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));
    }

    @Test
    void calculate_classificationMissing_returns404() throws Exception {
        when(taxService.calculate(any()))
                .thenThrow(new TaxClassificationNotFoundException("No tax classification found for product BOS-001 on 2026-10-03"));

        mockMvc.perform(post("/api/v1/taxes/calculate").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("TAX_CLASSIFICATION_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/v1/taxes/calculate"));
    }

    @Test
    void calculate_invalidState_returns400() throws Exception {
        when(taxService.calculate(any())).thenThrow(new InvalidStateException("sellerState 'XX' is not valid"));

        mockMvc.perform(post("/api/v1/taxes/calculate").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_STATE"));
    }

    @Test
    void calculate_rateNotVerified_returns422() throws Exception {
        when(taxService.calculate(any())).thenThrow(new TaxRateNotVerifiedException("rate not verified"));

        mockMvc.perform(post("/api/v1/taxes/calculate").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("TAX_RATE_NOT_VERIFIED"));
    }

    @Test
    void getTaxRule_returns200() throws Exception {
        TaxRuleResponse rule = new TaxRuleResponse("TR-8467", "8467", "HEADING", "Tools",
                new BigDecimal("9.00"), new BigDecimal("9.00"), new BigDecimal("18.00"), new BigDecimal("0.00"),
                LocalDate.of(2026, 10, 1), null, "HEADING_RATE_VERIFIED", "CBIC GST rate schedule");
        when(taxService.getTaxRule("8467", null)).thenReturn(rule);

        mockMvc.perform(get("/api/v1/tax-rules/8467"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taxRuleId").value("TR-8467"))
                .andExpect(jsonPath("$.igstRate").value(18.00));
    }

    @Test
    void getClassification_returns200() throws Exception {
        TaxClassificationResponse c = new TaxClassificationResponse("BOS-001", "8467", "HEADING", "Tools",
                "HEADING/RATE VERIFIED; EXACT SUBHEADING CHECK", "CBIC GST rate schedule",
                LocalDate.of(2026, 10, 1), null, "ACTIVE");
        when(taxService.getClassification("BOS-001", null)).thenReturn(c);

        mockMvc.perform(get("/api/v1/tax-classifications/product/BOS-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value("BOS-001"))
                .andExpect(jsonPath("$.hsnCode").value("8467"));
    }
}
