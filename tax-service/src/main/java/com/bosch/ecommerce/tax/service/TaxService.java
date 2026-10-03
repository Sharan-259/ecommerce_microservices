package com.bosch.ecommerce.tax.service;

import com.bosch.ecommerce.tax.dto.TaxCalculationRequest;
import com.bosch.ecommerce.tax.dto.TaxCalculationResponse;
import com.bosch.ecommerce.tax.dto.TaxClassificationResponse;
import com.bosch.ecommerce.tax.dto.TaxRuleResponse;
import com.bosch.ecommerce.tax.entity.ProductTaxMapping;
import com.bosch.ecommerce.tax.entity.TaxRule;
import com.bosch.ecommerce.tax.exception.InvalidStateException;
import com.bosch.ecommerce.tax.exception.InvalidTaxableAmountException;
import com.bosch.ecommerce.tax.exception.InvalidTransactionDateException;
import com.bosch.ecommerce.tax.exception.TaxClassificationNotFoundException;
import com.bosch.ecommerce.tax.exception.TaxRateNotVerifiedException;
import com.bosch.ecommerce.tax.exception.TaxRuleNotFoundException;
import com.bosch.ecommerce.tax.mapper.TaxMapper;
import com.bosch.ecommerce.tax.repository.ProductTaxMappingRepository;
import com.bosch.ecommerce.tax.repository.TaxRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class TaxService {

    private static final Logger log = LoggerFactory.getLogger(TaxService.class);

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    /** GST came into force on 1 July 2017; earlier transaction dates are rejected. */
    private static final LocalDate GST_START_DATE = LocalDate.of(2017, 7, 1);
    private static final int MAX_FUTURE_DAYS = 366;

    public static final String TAX_TYPE_IGST = "IGST";
    public static final String TAX_TYPE_CGST_SGST = "CGST_SGST";

    private final TaxRuleRepository taxRuleRepository;
    private final ProductTaxMappingRepository mappingRepository;
    private final TaxMapper taxMapper;
    private final Clock clock;

    public TaxService(TaxRuleRepository taxRuleRepository,
                      ProductTaxMappingRepository mappingRepository,
                      TaxMapper taxMapper,
                      Clock clock) {
        this.taxRuleRepository = taxRuleRepository;
        this.mappingRepository = mappingRepository;
        this.taxMapper = taxMapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public TaxCalculationResponse calculate(TaxCalculationRequest request) {
        String productId = request.productId().trim();
        BigDecimal amount = validateAmount(request.taxableAmount());
        LocalDate date = validateDate(request.transactionDate());
        String seller = validateState(request.sellerState(), "sellerState");
        String buyer = validateState(request.buyerState(), "buyerState");

        // 1. product tax classification
        ProductTaxMapping mapping = mappingRepository.findEffectiveOn(productId, date).stream().findFirst()
                .orElseThrow(() -> new TaxClassificationNotFoundException(
                        "No tax classification found for product " + productId + " on " + date));

        // 2 + 3. applicable tax rule, checked against the effective date
        TaxRule rule = taxRuleRepository.findEffectiveOn(mapping.getHsnCode(), date).stream().findFirst()
                .orElseThrow(() -> new TaxRuleNotFoundException(
                        "No tax rule found for HSN " + mapping.getHsnCode() + " on " + date));

        if (!TaxRule.STATUS_HEADING_RATE_VERIFIED.equals(rule.getStatus())
                || rule.getCgstRate() == null || rule.getSgstRate() == null
                || rule.getIgstRate() == null || rule.getCessRate() == null) {
            throw new TaxRateNotVerifiedException("The GST rate for HSN " + rule.getHsnCode()
                    + " is not verified (rule status: " + rule.getStatus() + "). "
                    + "Product-specific details are required before tax can be calculated for product " + productId);
        }

        // 4. intra-state vs inter-state
        boolean intraState = seller.equals(buyer);

        // 5. calculation (rates always come from the database rule)
        BigDecimal cgstRate = ZERO;
        BigDecimal sgstRate = ZERO;
        BigDecimal igstRate = ZERO;
        BigDecimal cgstAmount = ZERO;
        BigDecimal sgstAmount = ZERO;
        BigDecimal igstAmount = ZERO;
        String taxType;

        if (intraState) {
            taxType = TAX_TYPE_CGST_SGST;
            cgstRate = rule.getCgstRate();
            sgstRate = rule.getSgstRate();
            cgstAmount = percentOf(amount, cgstRate);
            sgstAmount = percentOf(amount, sgstRate);
        } else {
            taxType = TAX_TYPE_IGST;
            igstRate = rule.getIgstRate();
            igstAmount = percentOf(amount, igstRate);
        }
        BigDecimal cessRate = rule.getCessRate();
        BigDecimal cessAmount = percentOf(amount, cessRate);

        BigDecimal taxAmount = cgstAmount.add(sgstAmount).add(igstAmount).add(cessAmount);
        BigDecimal totalAmount = amount.add(taxAmount);

        List<String> warnings = new ArrayList<>();
        if ("HEADING".equals(mapping.getHsnLevel())) {
            warnings.add("HSN " + mapping.getHsnCode() + " is a 4-digit heading/candidate; "
                    + "the exact product-level HSN must be verified before real invoicing.");
        }
        warnings.add("Demonstration data only - not for real invoicing or tax compliance.");

        log.info("Tax calculated for product {} hsn={} type={} taxable={} tax={}",
                productId, mapping.getHsnCode(), taxType, amount, taxAmount);

        return new TaxCalculationResponse(
                productId, amount, mapping.getHsnCode(), taxType,
                cgstRate, sgstRate, igstRate, cessRate,
                cgstAmount, sgstAmount, igstAmount, cessAmount,
                taxAmount, totalAmount,
                rule.getTaxRuleId(), mapping.getVerificationStatus(), List.copyOf(warnings));
    }

    @Transactional(readOnly = true)
    public TaxRuleResponse getTaxRule(String hsnCode, LocalDate date) {
        LocalDate effectiveDate = date != null ? date : LocalDate.now(clock);
        TaxRule rule = taxRuleRepository.findEffectiveOn(hsnCode.trim(), effectiveDate).stream().findFirst()
                .orElseThrow(() -> new TaxRuleNotFoundException(
                        "No tax rule found for HSN " + hsnCode + " on " + effectiveDate));
        return taxMapper.toRuleResponse(rule);
    }

    @Transactional(readOnly = true)
    public TaxClassificationResponse getClassification(String productId, LocalDate date) {
        LocalDate effectiveDate = date != null ? date : LocalDate.now(clock);
        ProductTaxMapping mapping = mappingRepository.findEffectiveOn(productId.trim(), effectiveDate).stream().findFirst()
                .orElseThrow(() -> new TaxClassificationNotFoundException(
                        "No tax classification found for product " + productId + " on " + effectiveDate));
        return taxMapper.toClassificationResponse(mapping);
    }

    private BigDecimal validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidTaxableAmountException("taxableAmount must be greater than 0");
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new InvalidTaxableAmountException("taxableAmount must have at most 2 decimal places");
        }
        return amount.setScale(2, RoundingMode.UNNECESSARY);
    }

    private LocalDate validateDate(LocalDate date) {
        if (date == null) {
            throw new InvalidTransactionDateException("transactionDate is required");
        }
        if (date.isBefore(GST_START_DATE)) {
            throw new InvalidTransactionDateException("transactionDate " + date
                    + " is before the start of GST (" + GST_START_DATE + ")");
        }
        if (date.isAfter(LocalDate.now(clock).plusDays(MAX_FUTURE_DAYS))) {
            throw new InvalidTransactionDateException("transactionDate " + date
                    + " is more than " + MAX_FUTURE_DAYS + " days in the future");
        }
        return date;
    }

    private String validateState(String state, String fieldName) {
        String normalized = state == null ? "" : state.trim().toUpperCase(Locale.ROOT);
        if (!IndianStates.isValid(normalized)) {
            throw new InvalidStateException(fieldName + " '" + state
                    + "' is not a valid 2-letter Indian state/UT code (examples: KA, MH, DL, TN)");
        }
        return normalized;
    }

    private BigDecimal percentOf(BigDecimal amount, BigDecimal ratePercent) {
        return amount.multiply(ratePercent).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }
}
