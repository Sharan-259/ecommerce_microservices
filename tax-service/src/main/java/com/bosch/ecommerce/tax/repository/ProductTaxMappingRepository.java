package com.bosch.ecommerce.tax.repository;

import com.bosch.ecommerce.tax.entity.ProductTaxMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ProductTaxMappingRepository extends JpaRepository<ProductTaxMapping, Long> {

    /** Active classification of a product valid on a date. */
    @Query("""
            select m from ProductTaxMapping m
            where m.productId = :productId
              and m.status = 'ACTIVE'
              and m.effectiveFrom <= :date
              and (m.effectiveTo is null or m.effectiveTo >= :date)
            order by m.effectiveFrom desc
            """)
    List<ProductTaxMapping> findEffectiveOn(@Param("productId") String productId, @Param("date") LocalDate date);
}
