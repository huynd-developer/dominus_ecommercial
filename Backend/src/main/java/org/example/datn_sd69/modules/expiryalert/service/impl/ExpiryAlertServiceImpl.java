package org.example.datn_sd69.modules.expiryalert.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.enums.ExpiryAlertGroup;
import org.example.datn_sd69.modules.expiryalert.dto.response.ExpiryAlertListResponse;
import org.example.datn_sd69.modules.expiryalert.dto.response.ExpiryAlertSummaryResponse;
import org.example.datn_sd69.modules.expiryalert.service.ExpiryAlertService;
import org.example.datn_sd69.modules.inventorylot.dto.response.InventoryLotDetailResponse;
import org.example.datn_sd69.modules.inventorylot.service.InventoryLotService;
import org.example.datn_sd69.repository.ExpiryAlertRepository;
import org.example.datn_sd69.repository.projection.ExpiryAlertSummaryProjection;
import org.example.datn_sd69.repository.projection.ExpiryAlertViewProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ExpiryAlertServiceImpl implements ExpiryAlertService {

    private final ExpiryAlertRepository expiryAlertRepository;

    private final InventoryLotService inventoryLotService;


    @Override
    @Transactional(readOnly = true)
    public Page<ExpiryAlertListResponse> getList(
            ExpiryAlertGroup group,
            String keyword,
            Integer fromDays,
            Integer toDays,
            Pageable pageable
    ) {

        ExpiryAlertGroup normalizedGroup =
                group == null
                        ? ExpiryAlertGroup.NEAR_EXPIRY
                        : group;

        validateDayRange(fromDays, toDays);

        return expiryAlertRepository.search(
                normalizedGroup.name(),
                normalizeOptional(keyword),
                fromDays,
                toDays,
                pageable
        ).map(this::mapList);
    }


    @Override
    @Transactional(readOnly = true)
    public ExpiryAlertSummaryResponse getSummary() {

        ExpiryAlertSummaryProjection projection =
                expiryAlertRepository.getSummary();

        if (projection == null
                || projection.getWarningDays() == null) {

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Không tìm thấy cấu hình cảnh báo hạn sử dụng."
            );
        }

        return ExpiryAlertSummaryResponse.builder()

                .warningDays(
                        projection.getWarningDays()
                )

                .nearExpiryLotCount(
                        safeLong(projection.getNearExpiryLotCount())
                )

                .nearExpiryQuantity(
                        safeLong(projection.getNearExpiryQuantity())
                )

                .expiredLotCount(
                        safeLong(projection.getExpiredLotCount())
                )

                .expiredQuantity(
                        safeLong(projection.getExpiredQuantity())
                )

                .build();
    }


    @Override
    @Transactional(readOnly = true)
    public InventoryLotDetailResponse getDetail(Integer id) {

        validateId(id);

        return inventoryLotService.getDetail(id);
    }


    private ExpiryAlertListResponse mapList(
            ExpiryAlertViewProjection projection
    ) {

        return ExpiryAlertListResponse.builder()

                .id(
                        projection.getId()
                )

                .productVariantId(
                        projection.getProductVariantId()
                )

                .sku(
                        projection.getSku()
                )

                .productName(
                        projection.getProductName()
                )

                .imageUrl(
                        projection.getImageUrl()
                )

                .capacityValue(
                        projection.getCapacityValue()
                )

                .bottleTypeName(
                        projection.getBottleTypeName()
                )

                .lotCode(
                        projection.getLotCode()
                )

                .quantityOnHand(
                        projection.getQuantityOnHand()
                )

                .sellableQuantity(
                        projection.getSellableQuantity()
                )

                .expirationDate(
                        projection.getExpirationDate()
                )

                .daysToExpiry(
                        projection.getDaysToExpiry()
                )

                .isNearExpiry(
                        projection.getIsNearExpiry()
                )

                .isExpired(
                        projection.getIsExpired()
                )

                .build();
    }


    private void validateDayRange(
            Integer fromDays,
            Integer toDays
    ) {

        if (fromDays != null
                && toDays != null
                && fromDays > toDays) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Số ngày bắt đầu không được lớn hơn số ngày kết thúc."
            );
        }
    }


    private void validateId(Integer id) {

        if (id == null || id <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Id lô hàng không hợp lệ."
            );
        }
    }


    private String normalizeOptional(String value) {

        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }


    private Long safeLong(Long value) {

        return value == null
                ? 0L
                : value;
    }
}