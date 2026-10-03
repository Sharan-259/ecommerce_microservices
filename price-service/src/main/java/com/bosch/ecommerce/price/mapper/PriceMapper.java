package com.bosch.ecommerce.price.mapper;

import com.bosch.ecommerce.price.dto.PriceHistoryResponse;
import com.bosch.ecommerce.price.dto.PriceResponse;
import com.bosch.ecommerce.price.entity.ProductPrice;
import org.springframework.stereotype.Component;

/** Hand-written mapper: keeps API DTOs decoupled from JPA entities. */
@Component
public class PriceMapper {

    public PriceResponse toResponse(ProductPrice p) {
        return new PriceResponse(
                p.getProductId(),
                p.getCurrency(),
                p.getBasePrice(),
                p.getSellingPrice(),
                p.getDiscountPercent(),
                p.getPriceVersion(),
                p.getValidFrom(),
                p.getValidTo(),
                p.getStatus().name());
    }

    public PriceHistoryResponse toHistoryResponse(ProductPrice p) {
        return new PriceHistoryResponse(
                p.getPriceId(),
                p.getProductId(),
                p.getCurrency(),
                p.getBasePrice(),
                p.getSellingPrice(),
                p.getDiscountPercent(),
                p.getPriceVersion(),
                p.getValidFrom(),
                p.getValidTo(),
                p.getStatus().name(),
                p.getCreatedAt(),
                p.getUpdatedAt());
    }
}
