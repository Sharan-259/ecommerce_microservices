package com.bosch.ecommerce.price.repository;

import com.bosch.ecommerce.price.entity.ProductPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PriceRepository extends JpaRepository<ProductPrice, Long> {

    /**
     * Effective-dated lookup:
     * valid_from <= date AND (valid_to IS NULL OR valid_to >= date).
     * Ordered by version desc as a defensive tie-breaker; the service guarantees that
     * validity windows of one product never overlap.
     */
    @Query("""
            select p from ProductPrice p
            where p.productId = :productId
              and p.validFrom <= :date
              and (p.validTo is null or p.validTo >= :date)
            order by p.priceVersion desc
            """)
    List<ProductPrice> findEffectiveOn(@Param("productId") String productId,
                                       @Param("date") LocalDate date);

    /** The latest version of a product (highest priceVersion). */
    Optional<ProductPrice> findTopByProductIdOrderByPriceVersionDesc(String productId);

    /** Full history, oldest version first. */
    List<ProductPrice> findByProductIdOrderByPriceVersionAsc(String productId);
}
