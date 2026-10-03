package com.bosch.ecommerce.price.service;

import com.bosch.ecommerce.price.dto.CreatePriceRequest;
import com.bosch.ecommerce.price.dto.PriceHistoryResponse;
import com.bosch.ecommerce.price.dto.PriceResponse;
import com.bosch.ecommerce.price.entity.PriceStatus;
import com.bosch.ecommerce.price.entity.ProductPrice;
import com.bosch.ecommerce.price.exception.InvalidPriceException;
import com.bosch.ecommerce.price.exception.PriceConflictException;
import com.bosch.ecommerce.price.exception.PriceNotFoundException;
import com.bosch.ecommerce.price.mapper.PriceMapper;
import com.bosch.ecommerce.price.repository.PriceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriceServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 3);

    @Mock
    private PriceRepository repository;

    private PriceService service;

    @BeforeEach
    void setUp() {
        Clock fixed = Clock.fixed(Instant.parse("2026-10-03T10:00:00Z"), ZoneId.of("UTC"));
        service = new PriceService(repository, new PriceMapper(), fixed);
    }

    private ProductPrice price(String productId, int version, String base, String selling, String discount,
                               LocalDate from, LocalDate to, PriceStatus status) {
        ProductPrice p = new ProductPrice();
        p.setPriceId((long) version);
        p.setProductId(productId);
        p.setCurrency("INR");
        p.setBasePrice(new BigDecimal(base));
        p.setSellingPrice(new BigDecimal(selling));
        p.setDiscountPercent(new BigDecimal(discount));
        p.setValidFrom(from);
        p.setValidTo(to);
        p.setPriceVersion(version);
        p.setStatus(status);
        p.setCreatedAt(LocalDateTime.of(2026, 10, 1, 0, 0));
        p.setUpdatedAt(LocalDateTime.of(2026, 10, 1, 0, 0));
        return p;
    }

    private CreatePriceRequest request(String base, String selling, String discount, LocalDate validFrom) {
        return new CreatePriceRequest("BOS-001", "INR", new BigDecimal(base), new BigDecimal(selling),
                new BigDecimal(discount), validFrom);
    }

    // ---------- current price ----------

    @Test
    void getCurrentPrice_returnsPriceEffectiveToday() {
        ProductPrice v1 = price("BOS-001", 1, "6999.00", "6999.00", "0.00",
                LocalDate.of(2026, 10, 1), null, PriceStatus.ACTIVE);
        when(repository.findEffectiveOn("BOS-001", TODAY)).thenReturn(List.of(v1));

        PriceResponse response = service.getCurrentPrice("BOS-001");

        assertThat(response.productId()).isEqualTo("BOS-001");
        assertThat(response.sellingPrice()).isEqualByComparingTo("6999.00");
        assertThat(response.priceVersion()).isEqualTo(1);
        assertThat(response.status()).isEqualTo("ACTIVE");
    }

    @Test
    void getCurrentPrice_notFound_throws() {
        when(repository.findEffectiveOn("NOPE", TODAY)).thenReturn(List.of());

        assertThatThrownBy(() -> service.getCurrentPrice("NOPE"))
                .isInstanceOf(PriceNotFoundException.class)
                .hasMessage("No active price found for product NOPE");
    }

    // ---------- effective-dated (historical) price ----------

    @Test
    void getPriceOn_returnsVersionValidOnThatDate() {
        LocalDate date = LocalDate.of(2026, 3, 15);
        ProductPrice v1 = price("BOS-001", 1, "10000.00", "10000.00", "0.00",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30), PriceStatus.SUPERSEDED);
        when(repository.findEffectiveOn("BOS-001", date)).thenReturn(List.of(v1));

        PriceResponse response = service.getPriceOn("BOS-001", date);

        assertThat(response.priceVersion()).isEqualTo(1);
        assertThat(response.sellingPrice()).isEqualByComparingTo("10000.00");
        assertThat(response.status()).isEqualTo("SUPERSEDED");
    }

    @Test
    void getPriceOn_noPriceValidOnDate_throws() {
        LocalDate date = LocalDate.of(2020, 1, 1);
        when(repository.findEffectiveOn("BOS-001", date)).thenReturn(List.of());

        assertThatThrownBy(() -> service.getPriceOn("BOS-001", date))
                .isInstanceOf(PriceNotFoundException.class)
                .hasMessageContaining("2020-01-01");
    }

    // ---------- history ----------

    @Test
    void getPriceHistory_returnsAllVersions() {
        ProductPrice v1 = price("BOS-001", 1, "6999.00", "6999.00", "0.00",
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2), PriceStatus.SUPERSEDED);
        ProductPrice v2 = price("BOS-001", 2, "7999.00", "7199.10", "10.00",
                LocalDate.of(2026, 10, 3), null, PriceStatus.ACTIVE);
        when(repository.findByProductIdOrderByPriceVersionAsc("BOS-001")).thenReturn(List.of(v1, v2));

        List<PriceHistoryResponse> history = service.getPriceHistory("BOS-001");

        assertThat(history).hasSize(2);
        assertThat(history.get(0).priceVersion()).isEqualTo(1);
        assertThat(history.get(0).status()).isEqualTo("SUPERSEDED");
        assertThat(history.get(1).priceVersion()).isEqualTo(2);
        assertThat(history.get(1).validTo()).isNull();
    }

    @Test
    void getPriceHistory_unknownProduct_throws() {
        when(repository.findByProductIdOrderByPriceVersionAsc("NOPE")).thenReturn(List.of());

        assertThatThrownBy(() -> service.getPriceHistory("NOPE"))
                .isInstanceOf(PriceNotFoundException.class);
    }

    // ---------- create + versioning ----------

    @Test
    void createPrice_firstPriceOfProduct_getsVersion1() {
        when(repository.findTopByProductIdOrderByPriceVersionDesc("BOS-001")).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(ProductPrice.class))).thenAnswer(inv -> inv.getArgument(0));

        PriceResponse response = service.createPrice(request("12000.00", "10800.00", "10.00", LocalDate.of(2026, 10, 1)));

        assertThat(response.priceVersion()).isEqualTo(1);
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.validTo()).isNull();
        verify(repository, times(1)).saveAndFlush(any(ProductPrice.class));
    }

    @Test
    void createPrice_closesOldVersionAndPreservesIt() {
        ProductPrice v1 = price("BOS-001", 1, "6999.00", "6999.00", "0.00",
                LocalDate.of(2026, 10, 1), null, PriceStatus.ACTIVE);
        when(repository.findTopByProductIdOrderByPriceVersionDesc("BOS-001")).thenReturn(Optional.of(v1));
        when(repository.saveAndFlush(any(ProductPrice.class))).thenAnswer(inv -> inv.getArgument(0));

        PriceResponse response = service.createPrice(request("7999.00", "7199.10", "10.00", LocalDate.of(2026, 10, 3)));

        // old version is closed, not deleted or overwritten
        assertThat(v1.getValidTo()).isEqualTo(LocalDate.of(2026, 10, 2));
        assertThat(v1.getStatus()).isEqualTo(PriceStatus.SUPERSEDED);
        assertThat(v1.getBasePrice()).isEqualByComparingTo("6999.00");

        // new version is generated by the service
        assertThat(response.priceVersion()).isEqualTo(2);
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.validFrom()).isEqualTo(LocalDate.of(2026, 10, 3));
        assertThat(response.validTo()).isNull();

        ArgumentCaptor<ProductPrice> captor = ArgumentCaptor.forClass(ProductPrice.class);
        verify(repository, times(2)).saveAndFlush(captor.capture());
        assertThat(captor.getAllValues()).extracting(ProductPrice::getPriceVersion).containsExactly(1, 2);
    }

    @Test
    void createPrice_validFromNotAfterCurrentVersion_throwsConflict() {
        ProductPrice v1 = price("BOS-001", 1, "6999.00", "6999.00", "0.00",
                LocalDate.of(2026, 10, 1), null, PriceStatus.ACTIVE);
        when(repository.findTopByProductIdOrderByPriceVersionDesc("BOS-001")).thenReturn(Optional.of(v1));

        assertThatThrownBy(() -> service.createPrice(request("7999.00", "7199.10", "10.00", LocalDate.of(2026, 10, 1))))
                .isInstanceOf(PriceConflictException.class);

        assertThat(v1.getStatus()).isEqualTo(PriceStatus.ACTIVE);
        assertThat(v1.getValidTo()).isNull();
        verify(repository, never()).saveAndFlush(any(ProductPrice.class));
    }

    @Test
    void createPrice_currencyDifferentFromExisting_throwsInvalidPrice() {
        ProductPrice v1 = price("BOS-001", 1, "6999.00", "6999.00", "0.00",
                LocalDate.of(2026, 10, 1), null, PriceStatus.ACTIVE);
        when(repository.findTopByProductIdOrderByPriceVersionDesc("BOS-001")).thenReturn(Optional.of(v1));

        CreatePriceRequest usd = new CreatePriceRequest("BOS-001", "usd", new BigDecimal("100.00"),
                new BigDecimal("100.00"), new BigDecimal("0.00"), LocalDate.of(2026, 10, 3));

        assertThatThrownBy(() -> service.createPrice(usd))
                .isInstanceOf(InvalidPriceException.class)
                .hasMessageContaining("Currency");
    }

    // ---------- business-rule validation ----------

    @Test
    void createPrice_sellingPriceAboveBasePrice_throwsInvalidPrice() {
        assertThatThrownBy(() -> service.createPrice(request("1000.00", "1200.00", "0.00", LocalDate.of(2026, 10, 3))))
                .isInstanceOf(InvalidPriceException.class)
                .hasMessageContaining("sellingPrice cannot be greater than basePrice");
        verify(repository, never()).saveAndFlush(any(ProductPrice.class));
    }

    @Test
    void createPrice_discountInconsistentWithSellingPrice_throwsInvalidPrice() {
        // 12000 with 10% discount must be 10800.00, not 9000.00
        assertThatThrownBy(() -> service.createPrice(request("12000.00", "9000.00", "10.00", LocalDate.of(2026, 10, 3))))
                .isInstanceOf(InvalidPriceException.class)
                .hasMessageContaining("10800.00");
    }

    @Test
    void createPrice_roundingWithinOnePaiseIsAccepted() {
        when(repository.findTopByProductIdOrderByPriceVersionDesc("BOS-001")).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(ProductPrice.class))).thenAnswer(inv -> inv.getArgument(0));

        // 8999 * 0.95 = 8549.05
        PriceResponse response = service.createPrice(request("8999.00", "8549.05", "5.00", LocalDate.of(2026, 10, 3)));

        assertThat(response.sellingPrice()).isEqualByComparingTo("8549.05");
    }
}
