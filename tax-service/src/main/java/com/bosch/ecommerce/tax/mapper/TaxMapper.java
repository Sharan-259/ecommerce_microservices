package com.bosch.ecommerce.tax.mapper;

import com.bosch.ecommerce.tax.dto.TaxClassificationResponse;
import com.bosch.ecommerce.tax.dto.TaxRuleResponse;
import com.bosch.ecommerce.tax.entity.ProductTaxMapping;
import com.bosch.ecommerce.tax.entity.TaxRule;
import org.springframework.stereotype.Component;

/** Hand-written mapper: keeps API DTOs decoupled from JPA entities. */
@Component
public class TaxMapper {

    public TaxRuleResponse toRuleResponse(TaxRule r) {
        return new TaxRuleResponse(
                r.getTaxRuleId(),
                r.getHsnCode(),
                r.getHsnLevel(),
                r.getHsnDescription(),
                r.getCgstRate(),
                r.getSgstRate(),
                r.getIgstRate(),
                r.getCessRate(),
                r.getEffectiveFrom(),
                r.getEffectiveTo(),
                r.getStatus(),
                r.getSourceReference());
    }

    public TaxClassificationResponse toClassificationResponse(ProductTaxMapping m) {
        return new TaxClassificationResponse(
                m.getProductId(),
                m.getHsnCode(),
                m.getHsnLevel(),
                m.getHsnDescription(),
                m.getVerificationStatus(),
                m.getTaxSource(),
                m.getEffectiveFrom(),
                m.getEffectiveTo(),
                m.getStatus());
    }
}
