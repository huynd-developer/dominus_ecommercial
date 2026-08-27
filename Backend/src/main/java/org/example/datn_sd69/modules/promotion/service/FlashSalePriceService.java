package org.example.datn_sd69.modules.promotion.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FlashSalePriceService {

    private final EntityManager entityManager;

    public BigDecimal findActiveFlashSalePercent(Integer variantId) {
        if (variantId == null || variantId <= 0) {
            return BigDecimal.ZERO;
        }

        LocalDateTime now = LocalDateTime.now();

        List<?> result = entityManager.createNativeQuery(
                        """
                        SELECT TOP (1) CAST(pv.DiscountPercent AS DECIMAL(5,2))
                        FROM dbo.PromotionVariant pv
                        JOIN dbo.Promotion p ON p.Id = pv.PromotionId
                        WHERE pv.ProductVariantId = :variantId
                          AND p.Status = 1
                          AND p.StartDate <= :now
                          AND p.EndDate >= :now
                          AND pv.DiscountPercent IS NOT NULL
                          AND pv.DiscountPercent > 0
                        ORDER BY pv.DiscountPercent DESC
                        """
                )
                .setParameter("variantId", variantId)
                .setParameter("now", now)
                .getResultList();

        if (result == null || result.isEmpty() || result.get(0) == null) {
            return BigDecimal.ZERO;
        }

        Object value = result.get(0);

        if (value instanceof BigDecimal decimalValue) {
            return decimalValue.setScale(2, RoundingMode.HALF_UP);
        }

        if (value instanceof Number numberValue) {
            return BigDecimal.valueOf(numberValue.doubleValue())
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return new BigDecimal(value.toString())
                .setScale(2, RoundingMode.HALF_UP);
    }

    // THÊM METHOD NÀY ĐỂ SỬA LỖI Ở ORDER SERVICE
    public BigDecimal getEffectiveFlashSalePrice(Integer variantId) {
        BigDecimal discountPercent = findActiveFlashSalePercent(variantId);
        if (discountPercent == null || discountPercent.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        try {
            List<?> result = entityManager.createQuery(
                            "SELECT pv.price FROM ProductVariant pv WHERE pv.id = :variantId"
                    )
                    .setParameter("variantId", variantId)
                    .getResultList();

            if (result == null || result.isEmpty() || result.get(0) == null) {
                return null;
            }

            BigDecimal originalPrice = (BigDecimal) result.get(0);
            BigDecimal discountFactor = BigDecimal.ONE.subtract(
                    discountPercent.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)
            );

            return originalPrice.multiply(discountFactor).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return null;
        }
    }
}