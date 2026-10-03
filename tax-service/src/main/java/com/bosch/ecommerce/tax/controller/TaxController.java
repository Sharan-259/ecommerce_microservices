package com.bosch.ecommerce.tax.controller;

import com.bosch.ecommerce.tax.dto.TaxCalculationRequest;
import com.bosch.ecommerce.tax.dto.TaxCalculationResponse;
import com.bosch.ecommerce.tax.dto.TaxClassificationResponse;
import com.bosch.ecommerce.tax.dto.TaxRuleResponse;
import com.bosch.ecommerce.tax.service.TaxService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Tax", description = "Tax classification, tax rules and GST calculation")
public class TaxController {

    private final TaxService taxService;

    public TaxController(TaxService taxService) {
        this.taxService = taxService;
    }

    @Operation(summary = "Calculate GST for a product and taxable amount")
    @PostMapping("/taxes/calculate")
    public TaxCalculationResponse calculate(@Valid @RequestBody TaxCalculationRequest request) {
        return taxService.calculate(request);
    }

    @Operation(summary = "Get the tax rule of an HSN code (today, or on a given date)")
    @GetMapping("/tax-rules/{hsnCode}")
    public TaxRuleResponse getTaxRule(
            @PathVariable("hsnCode") String hsnCode,
            @Parameter(description = "Optional effective date, format yyyy-MM-dd", example = "2026-10-03")
            @RequestParam(name = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return taxService.getTaxRule(hsnCode, date);
    }

    @Operation(summary = "Get the tax classification (HSN mapping) of a product")
    @GetMapping("/tax-classifications/product/{productId}")
    public TaxClassificationResponse getClassification(
            @PathVariable("productId") String productId,
            @Parameter(description = "Optional effective date, format yyyy-MM-dd", example = "2026-10-03")
            @RequestParam(name = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return taxService.getClassification(productId, date);
    }
}
