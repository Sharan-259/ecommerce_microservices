package com.bosch.ecommerce.price.controller;

import com.bosch.ecommerce.price.dto.PriceHistoryResponse;
import com.bosch.ecommerce.price.dto.PriceResponse;
import com.bosch.ecommerce.price.exception.GlobalExceptionHandler;
import com.bosch.ecommerce.price.exception.PriceNotFoundException;
import com.bosch.ecommerce.price.service.PriceService;
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
import java.time.LocalDateTime;
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
class PriceControllerTest {

    @Mock
    private PriceService priceService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PriceController(priceService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private PriceResponse sample() {
        return new PriceResponse("BOS-001", "INR", new BigDecimal("6999.00"), new BigDecimal("6999.00"),
                new BigDecimal("0.00"), 1, LocalDate.of(2026, 10, 1), null, "ACTIVE");
    }

    @Test
    void getCurrentPrice_returns200() throws Exception {
        when(priceService.getCurrentPrice("BOS-001")).thenReturn(sample());

        mockMvc.perform(get("/api/v1/prices/BOS-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value("BOS-001"))
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.priceVersion").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getPriceOnDate_usesDateParameter() throws Exception {
        LocalDate date = LocalDate.of(2026, 10, 15);
        when(priceService.getPriceOn("BOS-001", date)).thenReturn(sample());

        mockMvc.perform(get("/api/v1/prices/BOS-001").param("date", "2026-10-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value("BOS-001"));

        verify(priceService).getPriceOn("BOS-001", date);
        verify(priceService, never()).getCurrentPrice(any());
    }

    @Test
    void getPrice_malformedDate_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/prices/BOS-001").param("date", "03-10-2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_PARAMETER"));
    }

    @Test
    void getPrice_notFound_returnsStructuredError() throws Exception {
        when(priceService.getCurrentPrice("NOPE"))
                .thenThrow(new PriceNotFoundException("No active price found for product NOPE"));

        mockMvc.perform(get("/api/v1/prices/NOPE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("PRICE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("No active price found for product NOPE"))
                .andExpect(jsonPath("$.path").value("/api/v1/prices/NOPE"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void getHistory_returnsList() throws Exception {
        PriceHistoryResponse row = new PriceHistoryResponse(10001L, "BOS-001", "INR", new BigDecimal("6999.00"),
                new BigDecimal("6999.00"), new BigDecimal("0.00"), 1, LocalDate.of(2026, 10, 1), null, "ACTIVE",
                LocalDateTime.of(2026, 10, 1, 0, 0), LocalDateTime.of(2026, 10, 1, 0, 0));
        when(priceService.getPriceHistory("BOS-001")).thenReturn(List.of(row));

        mockMvc.perform(get("/api/v1/prices/BOS-001/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].priceVersion").value(1))
                .andExpect(jsonPath("$[0].productId").value("BOS-001"));
    }

    @Test
    void createPrice_validRequest_returns201() throws Exception {
        PriceResponse created = new PriceResponse("BOS-001", "INR", new BigDecimal("7999.00"),
                new BigDecimal("7199.10"), new BigDecimal("10.00"), 2, LocalDate.of(2026, 10, 3), null, "ACTIVE");
        when(priceService.createPrice(any())).thenReturn(created);

        String body = """
                {"productId":"BOS-001","currency":"INR","basePrice":7999.00,"sellingPrice":7199.10,
                 "discountPercent":10.00,"validFrom":"2026-10-03"}
                """;

        mockMvc.perform(post("/api/v1/prices").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.priceVersion").value(2))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void createPrice_missingAndInvalidFields_returns400WithDetails() throws Exception {
        String body = """
                {"currency":"INR","basePrice":-5.00,"sellingPrice":10.00,"discountPercent":150.00}
                """;

        mockMvc.perform(post("/api/v1/prices").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details").isArray());

        verify(priceService, never()).createPrice(any());
    }

    @Test
    void createPrice_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/prices").contentType(MediaType.APPLICATION_JSON).content("{ not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));
    }
}
