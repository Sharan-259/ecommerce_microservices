package com.bosch.ecommerce.tax.repository;

import com.bosch.ecommerce.tax.entity.TaxRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TaxRuleRepository extends JpaRepository<TaxRule, String> {

    /** Rule for an HSN code valid on a date: effective_from <= date AND (effective_to IS NULL OR effective_to >= date). */
    @Query("""
            select r from TaxRule r
            where r.hsnCode = :hsnCode
              and r.effectiveFrom <= :date
              and (r.effectiveTo is null or r.effectiveTo >= :date)
            order by r.effectiveFrom desc
            """)
    List<TaxRule> findEffectiveOn(@Param("hsnCode") String hsnCode, @Param("date") LocalDate date);
}
