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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaxServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 3);

    @Mock
    private TaxRuleRepository ruleRepository;

    @Mock
    private ProductTaxMappingRepository mappingRepository;

    private TaxService service;

    @BeforeEach
    void setUp() {
        Clock fixed = Clock.fixed(Instant.parse("2026-10-03T10:00:00Z"), ZoneId.of("UTC"));
        service = new TaxService(ruleRepository, mappingRepository, new TaxMapper(), fixed);
    }

    private ProductTaxMapping mapping(String productId, String hsn) {
        ProductTaxMapping m = new ProductTaxMapping();
        m.setMappingId(1L);
        m.setProductId(productId);
        m.setHsnCode(hsn);
        m.setHsnLevel("HEADING");
        m.setHsnDescription("Tools for working in the hand with self-contained motor");
        m.setVerificationStatus("HEADING/RATE VERIFIED; EXACT SUBHEADING CHECK");
        m.setTaxSource("CBIC GST rate schedule");
        m.setEffectiveFrom(LocalDate.of(2026, 10, 1));
        m.setStatus("ACTIVE");
        m.setCreatedAt(LocalDateTime.now());
        m.setUpdatedAt(LocalDateTime.now());
        return m;
    }

    private TaxRule rule(String id, String hsn, String cgst, String sgst, String igst, String cess, String status) {
        TaxRule r = new TaxRule();
        r.setTaxRuleId(id);
        r.setHsnCode(hsn);
        r.setHsnLevel("HEADING");
        r.setHsnDescription("test rule");
        r.setCgstRate(cgst == null ? null : new BigDecimal(cgst));
        r.setSgstRate(sgst == null ? null : new BigDecimal(sgst));
        r.setIgstRate(igst == null ? null : new BigDecimal(igst));
        r.setCessRate(cess == null ? null : new BigDecimal(cess));
        r.setEffectiveFrom(LocalDate.of(2026, 10, 1));
        r.setStatus(status);
        r.setSourceReference("test");
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        return r;
    }

    private TaxCalculationRequest req(String productId, String amount, String seller, String buyer, LocalDate date) {
        return new TaxCalculationRequest(productId, amount == null ? null : new BigDecimal(amount), seller, buyer, date);
    }

    private void givenVerifiedRule(String productId, String hsn, String cgst, String sgst, String igst, String cess) {
        when(mappingRepository.findEffectiveOn(productId, TODAY)).thenReturn(List.of(mapping(productId, hsn)));
        when(ruleRepository.findEffectiveOn(hsn, TODAY))
                .thenReturn(List.of(rule("TR-" + hsn, hsn, cgst, sgst, igst, cess, TaxRule.STATUS_HEADING_RATE_VERIFIED)));
    }

    // ---------- valid calculations ----------

    @Test
    void interState_usesIgst() {
        givenVerifiedRule("BOS-001", "8467", "9.00", "9.00", "18.00", "0.00");

        TaxCalculationResponse r = service.calculate(req("BOS-001", "10800.00", "KA", "MH", TODAY));

        assertThat(r.taxType()).isEqualTo("IGST");
        assertThat(r.cgstRate()).isEqualByComparingTo("0.00");
        assertThat(r.sgstRate()).isEqualByComparingTo("0.00");
        assertThat(r.igstRate()).isEqualByComparingTo("18.00");
        assertThat(r.cessRate()).isEqualByComparingTo("0.00");
        assertThat(r.taxAmount()).isEqualByComparingTo("1944.00");
        assertThat(r.totalAmount()).isEqualByComparingTo("12744.00");
        assertThat(r.hsnCode()).isEqualTo("8467");
        assertThat(r.taxRuleId()).isEqualTo("TR-8467");
    }

    @Test
    void intraState_usesCgstPlusSgst() {
        givenVerifiedRule("BOS-001", "8467", "9.00", "9.00", "18.00", "0.00");

        TaxCalculationResponse r = service.calculate(req("BOS-001", "6999.00", "KA", "KA", TODAY));

        assertThat(r.taxType()).isEqualTo("CGST_SGST");
        assertThat(r.cgstRate()).isEqualByComparingTo("9.00");
        assertThat(r.sgstRate()).isEqualByComparingTo("9.00");
        assertThat(r.igstRate()).isEqualByComparingTo("0.00");
        assertThat(r.cgstAmount()).isEqualByComparingTo("629.91");
        assertThat(r.sgstAmount()).isEqualByComparingTo("629.91");
        assertThat(r.igstAmount()).isEqualByComparingTo("0.00");
        assertThat(r.taxAmount()).isEqualByComparingTo("1259.82");
        assertThat(r.totalAmount()).isEqualByComparingTo("8258.82");
    }

    @Test
    void ratesComeFromTheRule_notHardCoded() {
        // A hypothetical 12% rule must produce 12% tax: proves no 18% is hard-coded.
        givenVerifiedRule("P-12", "9999", "6.00", "6.00", "12.00", "0.00");

        TaxCalculationResponse r = service.calculate(req("P-12", "1000.00", "KA", "MH", TODAY));

        assertThat(r.igstRate()).isEqualByComparingTo("12.00");
        assertThat(r.taxAmount()).isEqualByComparingTo("120.00");
        assertThat(r.totalAmount()).isEqualByComparingTo("1120.00");
    }

    @Test
    void cessIsAddedWhenRuleHasCess() {
        givenVerifiedRule("P-CESS", "8888", "14.00", "14.00", "28.00", "12.00");

        TaxCalculationResponse r = service.calculate(req("P-CESS", "1000.00", "KA", "MH", TODAY));

        assertThat(r.cessAmount()).isEqualByComparingTo("120.00");
        assertThat(r.taxAmount()).isEqualByComparingTo("400.00");
        assertThat(r.totalAmount()).isEqualByComparingTo("1400.00");
    }

    @Test
    void stateCodesAreCaseInsensitiveAndTrimmed() {
        givenVerifiedRule("BOS-001", "8467", "9.00", "9.00", "18.00", "0.00");

        TaxCalculationResponse r = service.calculate(req("BOS-001", "100.00", " ka ", "KA", TODAY));

        assertThat(r.taxType()).isEqualTo("CGST_SGST");
    }

    @Test
    void headingLevelHsnProducesVerificationWarning() {
        givenVerifiedRule("BOS-001", "8467", "9.00", "9.00", "18.00", "0.00");

        TaxCalculationResponse r = service.calculate(req("BOS-001", "100.00", "KA", "MH", TODAY));

        assertThat(r.warnings()).anyMatch(w -> w.contains("exact product-level HSN must be verified"));
        assertThat(r.verificationStatus()).contains("EXACT SUBHEADING CHECK");
    }

    // ---------- missing data ----------

    @Test
    void missingClassification_throwsNotFound() {
        when(mappingRepository.findEffectiveOn("UNKNOWN", TODAY)).thenReturn(List.of());

        assertThatThrownBy(() -> service.calculate(req("UNKNOWN", "100.00", "KA", "MH", TODAY)))
                .isInstanceOf(TaxClassificationNotFoundException.class)
                .hasMessageContaining("UNKNOWN");
        verifyNoInteractions(ruleRepository);
    }

    @Test
    void missingTaxRule_throwsNotFound() {
        when(mappingRepository.findEffectiveOn("BOS-001", TODAY)).thenReturn(List.of(mapping("BOS-001", "8467")));
        when(ruleRepository.findEffectiveOn("8467", TODAY)).thenReturn(List.of());

        assertThatThrownBy(() -> service.calculate(req("BOS-001", "100.00", "KA", "MH", TODAY)))
                .isInstanceOf(TaxRuleNotFoundException.class)
                .hasMessageContaining("8467");
    }

    @Test
    void unverifiedRule_withoutRates_isRefused() {
        when(mappingRepository.findEffectiveOn("BOS-020", TODAY)).thenReturn(List.of(mapping("BOS-020", "8507")));
        when(ruleRepository.findEffectiveOn("8507", TODAY)).thenReturn(List.of(
                rule("TR-8507", "8507", null, null, null, null, TaxRule.STATUS_REQUIRES_PRODUCT_SPECIFICATION)));

        assertThatThrownBy(() -> service.calculate(req("BOS-020", "4249.15", "KA", "MH", TODAY)))
                .isInstanceOf(TaxRateNotVerifiedException.class)
                .hasMessageContaining("8507");
    }

    // ---------- invalid input ----------

    @Test
    void zeroAmount_isInvalid() {
        assertThatThrownBy(() -> service.calculate(req("BOS-001", "0.00", "KA", "MH", TODAY)))
                .isInstanceOf(InvalidTaxableAmountException.class);
    }

    @Test
    void negativeAmount_isInvalid() {
        assertThatThrownBy(() -> service.calculate(req("BOS-001", "-10.00", "KA", "MH", TODAY)))
                .isInstanceOf(InvalidTaxableAmountException.class);
    }

    @Test
    void amountWithMoreThanTwoDecimals_isInvalid() {
        assertThatThrownBy(() -> service.calculate(req("BOS-001", "10.005", "KA", "MH", TODAY)))
                .isInstanceOf(InvalidTaxableAmountException.class)
                .hasMessageContaining("2 decimal places");
    }

    @Test
    void dateBeforeGstStart_isInvalid() {
        assertThatThrownBy(() -> service.calculate(req("BOS-001", "100.00", "KA", "MH", LocalDate.of(2015, 1, 1))))
                .isInstanceOf(InvalidTransactionDateException.class);
    }

    @Test
    void dateFarInFuture_isInvalid() {
        assertThatThrownBy(() -> service.calculate(req("BOS-001", "100.00", "KA", "MH", LocalDate.of(2030, 1, 1))))
                .isInstanceOf(InvalidTransactionDateException.class);
    }

    @Test
    void nullDate_isInvalid() {
        assertThatThrownBy(() -> service.calculate(req("BOS-001", "100.00", "KA", "MH", null)))
                .isInstanceOf(InvalidTransactionDateException.class);
    }

    @Test
    void unknownSellerState_isInvalid() {
        assertThatThrownBy(() -> service.calculate(req("BOS-001", "100.00", "XX", "MH", TODAY)))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("sellerState");
    }

    @Test
    void unknownBuyerState_isInvalid() {
        assertThatThrownBy(() -> service.calculate(req("BOS-001", "100.00", "KA", "Karnataka", TODAY)))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("buyerState");
    }

    // ---------- lookups ----------

    @Test
    void getTaxRule_returnsRuleForHsn() {
        when(ruleRepository.findEffectiveOn("8467", TODAY)).thenReturn(List.of(
                rule("TR-8467", "8467", "9.00", "9.00", "18.00", "0.00", TaxRule.STATUS_HEADING_RATE_VERIFIED)));

        TaxRuleResponse r = service.getTaxRule("8467", null);

        assertThat(r.taxRuleId()).isEqualTo("TR-8467");
        assertThat(r.igstRate()).isEqualByComparingTo("18.00");
    }

    @Test
    void getTaxRule_unknownHsn_throws() {
        when(ruleRepository.findEffectiveOn("0000", TODAY)).thenReturn(List.of());

        assertThatThrownBy(() -> service.getTaxRule("0000", null)).isInstanceOf(TaxRuleNotFoundException.class);
    }

    @Test
    void getClassification_returnsMapping() {
        when(mappingRepository.findEffectiveOn("BOS-001", TODAY)).thenReturn(List.of(mapping("BOS-001", "8467")));

        TaxClassificationResponse r = service.getClassification("BOS-001", null);

        assertThat(r.productId()).isEqualTo("BOS-001");
        assertThat(r.hsnCode()).isEqualTo("8467");
        assertThat(r.hsnLevel()).isEqualTo("HEADING");
    }

    @Test
    void getClassification_unknownProduct_throws() {
        when(mappingRepository.findEffectiveOn("NOPE", TODAY)).thenReturn(List.of());

        assertThatThrownBy(() -> service.getClassification("NOPE", null))
                .isInstanceOf(TaxClassificationNotFoundException.class);
    }
}
