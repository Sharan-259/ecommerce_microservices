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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class PriceService {

    private static final Logger log = LoggerFactory.getLogger(PriceService.class);
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal ROUNDING_TOLERANCE = new BigDecimal("0.01");

    private final PriceRepository priceRepository;
    private final PriceMapper priceMapper;
    private final Clock clock;

    public PriceService(PriceRepository priceRepository, PriceMapper priceMapper, Clock clock) {
        this.priceRepository = priceRepository;
        this.priceMapper = priceMapper;
        this.clock = clock;
    }

    /** Price valid today. */
    @Transactional(readOnly = true)
    public PriceResponse getCurrentPrice(String productId) {
        LocalDate today = LocalDate.now(clock);
        List<ProductPrice> hits = priceRepository.findEffectiveOn(productId, today);
        if (hits.isEmpty()) {
            throw new PriceNotFoundException("No active price found for product " + productId);
        }
        return priceMapper.toResponse(hits.get(0));
    }

    /** Price valid on a given date (effective-dated; NOT simply the latest record). */
    @Transactional(readOnly = true)
    public PriceResponse getPriceOn(String productId, LocalDate date) {
        List<ProductPrice> hits = priceRepository.findEffectiveOn(productId, date);
        if (hits.isEmpty()) {
            throw new PriceNotFoundException("No price found for product " + productId + " on " + date);
        }
        return priceMapper.toResponse(hits.get(0));
    }

    /** All versions, oldest first. */
    @Transactional(readOnly = true)
    public List<PriceHistoryResponse> getPriceHistory(String productId) {
        List<ProductPrice> all = priceRepository.findByProductIdOrderByPriceVersionAsc(productId);
        if (all.isEmpty()) {
            throw new PriceNotFoundException("No price history found for product " + productId);
        }
        return all.stream().map(priceMapper::toHistoryResponse).toList();
    }

    /**
     * Creates a new price version:
     * 1. find the latest (current open-ended) version,
     * 2. close its validity period (valid_to = newValidFrom - 1 day, status SUPERSEDED),
     * 3. generate the next version number,
     * 4. insert the new version,
     * 5. the old record is preserved, never overwritten.
     */
    @Transactional
    public PriceResponse createPrice(CreatePriceRequest request) {
        String productId = request.productId().trim();
        String currency = request.currency().trim().toUpperCase(Locale.ROOT);

        validateBusinessRules(request);

        LocalDateTime now = LocalDateTime.now(clock);
        int nextVersion = 1;

        Optional<ProductPrice> latest = priceRepository.findTopByProductIdOrderByPriceVersionDesc(productId);
        if (latest.isPresent()) {
            ProductPrice current = latest.get();

            if (!current.getCurrency().equals(currency)) {
                throw new InvalidPriceException("Currency " + currency + " does not match existing currency "
                        + current.getCurrency() + " of product " + productId);
            }
            if (!request.validFrom().isAfter(current.getValidFrom())) {
                throw new PriceConflictException("validFrom " + request.validFrom()
                        + " must be later than validFrom " + current.getValidFrom()
                        + " of the current version " + current.getPriceVersion() + " of product " + productId);
            }
            if (current.getValidTo() == null) {
                current.setValidTo(request.validFrom().minusDays(1));
            } else if (!request.validFrom().isAfter(current.getValidTo())) {
                throw new PriceConflictException("validFrom " + request.validFrom()
                        + " overlaps the existing validity window of product " + productId);
            }
            current.setStatus(PriceStatus.SUPERSEDED);
            current.setUpdatedAt(now);
            priceRepository.saveAndFlush(current);
            nextVersion = current.getPriceVersion() + 1;
            log.info("Closed version {} of product {} (valid_to={})",
                    current.getPriceVersion(), productId, current.getValidTo());
        }

        ProductPrice created = new ProductPrice();
        created.setProductId(productId);
        created.setCurrency(currency);
        created.setBasePrice(request.basePrice());
        created.setSellingPrice(request.sellingPrice());
        created.setDiscountPercent(request.discountPercent());
        created.setValidFrom(request.validFrom());
        created.setValidTo(null);
        created.setPriceVersion(nextVersion);
        created.setStatus(PriceStatus.ACTIVE);
        created.setCreatedAt(now);
        created.setUpdatedAt(now);

        ProductPrice saved = priceRepository.saveAndFlush(created);
        log.info("Created version {} for product {} valid from {}", nextVersion, productId, request.validFrom());
        return priceMapper.toResponse(saved);
    }

    private void validateBusinessRules(CreatePriceRequest r) {
        if (r.sellingPrice().compareTo(r.basePrice()) > 0) {
            throw new InvalidPriceException("sellingPrice cannot be greater than basePrice");
        }
        BigDecimal expected = r.basePrice()
                .multiply(HUNDRED.subtract(r.discountPercent()))
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);
        if (r.sellingPrice().subtract(expected).abs().compareTo(ROUNDING_TOLERANCE) > 0) {
            throw new InvalidPriceException("sellingPrice must equal basePrice minus discountPercent (expected "
                    + expected.toPlainString() + ")");
        }
    }
}
