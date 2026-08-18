package org.example.datn_sd69.modules.stockmovement.service.impl;

import lombok.RequiredArgsConstructor;

import org.example.datn_sd69.enums.StockMovementType;

import org.example.datn_sd69.modules.stockmovement.dto.response.StockMovementDetailResponse;
import org.example.datn_sd69.modules.stockmovement.dto.response.StockMovementListResponse;

import org.example.datn_sd69.modules.stockmovement.service.StockMovementService;

import org.example.datn_sd69.repository.StockMovementRepository;

import org.example.datn_sd69.repository.projection.StockMovementViewProjection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.http.HttpStatus;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class StockMovementServiceImpl
        implements StockMovementService {

    private final StockMovementRepository stockMovementRepository;


    @Override
    @Transactional(readOnly = true)
    public Page<StockMovementListResponse> getList(

            String keyword,

            Integer inventoryLotId,

            StockMovementType movementType,

            Integer createdBy,

            String referenceType,

            Integer referenceId,

            LocalDate fromDate,

            LocalDate toDate,

            Pageable pageable
    ) {

        validateDateRange(
                fromDate,
                toDate
        );

        validateOptionalId(
                inventoryLotId,
                "Id lô hàng không hợp lệ."
        );

        validateOptionalId(
                createdBy,
                "Id người thao tác không hợp lệ."
        );

        validateOptionalId(
                referenceId,
                "Id chứng từ nguồn không hợp lệ."
        );


        LocalDateTime fromDateTime =
                fromDate == null
                        ? null
                        : fromDate.atStartOfDay();


        /*
         * Dùng ngày kế tiếp exclusive:
         *
         * toDate = 2026-08-13
         *
         * => CreatedAt < 2026-08-14 00:00
         */
        LocalDateTime toDateTime =
                toDate == null
                        ? null
                        : toDate
                        .plusDays(1)
                        .atStartOfDay();


        Byte movementTypeCode =
                movementType == null
                        ? null
                        : movementType.getCode();


        return stockMovementRepository
                .search(
                        normalizeOptional(keyword),

                        inventoryLotId,

                        movementTypeCode,

                        createdBy,

                        normalizeReferenceType(
                                referenceType
                        ),

                        referenceId,

                        fromDateTime,

                        toDateTime,

                        pageable
                )
                .map(this::mapList);
    }


    @Override
    @Transactional(readOnly = true)
    public StockMovementDetailResponse getDetail(
            Integer id
    ) {

        validateRequiredId(id);

        StockMovementViewProjection projection =
                stockMovementRepository
                        .findViewById(id)
                        .orElseThrow(
                                this::notFound
                        );

        return mapDetail(projection);
    }


    private StockMovementListResponse mapList(
            StockMovementViewProjection projection
    ) {

        StockMovementType movementType =
                StockMovementType.fromCode(
                        projection.getMovementType()
                );

        return StockMovementListResponse
                .builder()

                .id(
                        projection.getId()
                )

                .createdAt(
                        projection.getCreatedAt()
                )

                .inventoryLotId(
                        projection.getInventoryLotId()
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

                .lotCode(
                        projection.getLotCode()
                )

                .movementType(
                        movementType
                )

                .movementTypeLabel(
                        movementType == null
                                ? null
                                : movementType.getLabel()
                )

                .quantityChange(
                        projection.getQuantityChange()
                )

                .quantityBefore(
                        projection.getQuantityBefore()
                )

                .quantityAfter(
                        projection.getQuantityAfter()
                )

                .referenceType(
                        projection.getReferenceType()
                )

                .referenceId(
                        projection.getReferenceId()
                )

                .referenceLineId(
                        projection.getReferenceLineId()
                )

                .reason(
                        projection.getReason()
                )

                .createdById(
                        projection.getCreatedById()
                )

                .createdByName(
                        projection.getCreatedByName()
                )

                .build();
    }


    private StockMovementDetailResponse mapDetail(
            StockMovementViewProjection projection
    ) {

        StockMovementType movementType =
                StockMovementType.fromCode(
                        projection.getMovementType()
                );

        return StockMovementDetailResponse
                .builder()

                .id(
                        projection.getId()
                )

                .createdAt(
                        projection.getCreatedAt()
                )

                .inventoryLotId(
                        projection.getInventoryLotId()
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

                .lotCode(
                        projection.getLotCode()
                )

                .movementType(
                        movementType
                )

                .movementTypeLabel(
                        movementType == null
                                ? null
                                : movementType.getLabel()
                )

                .quantityChange(
                        projection.getQuantityChange()
                )

                .quantityBefore(
                        projection.getQuantityBefore()
                )

                .quantityAfter(
                        projection.getQuantityAfter()
                )

                .referenceType(
                        projection.getReferenceType()
                )

                .referenceId(
                        projection.getReferenceId()
                )

                .referenceLineId(
                        projection.getReferenceLineId()
                )

                .reason(
                        projection.getReason()
                )

                .createdById(
                        projection.getCreatedById()
                )

                .createdByName(
                        projection.getCreatedByName()
                )

                .build();
    }


    private void validateDateRange(
            LocalDate fromDate,
            LocalDate toDate
    ) {

        if (fromDate != null
                && toDate != null
                && fromDate.isAfter(toDate)) {

            throw badRequest(
                    "Từ ngày không được lớn hơn đến ngày."
            );
        }
    }


    private void validateRequiredId(
            Integer id
    ) {

        if (id == null || id <= 0) {

            throw badRequest(
                    "Id lịch sử kho không hợp lệ."
            );
        }
    }


    private void validateOptionalId(
            Integer id,
            String message
    ) {

        if (id != null && id <= 0) {

            throw badRequest(message);
        }
    }


    private String normalizeOptional(
            String value
    ) {

        if (value == null) {
            return null;
        }

        String normalized =
                value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }


    private String normalizeReferenceType(
            String value
    ) {

        String normalized =
                normalizeOptional(value);

        return normalized == null
                ? null
                : normalized.toUpperCase();
    }


    private ResponseStatusException badRequest(
            String message
    ) {

        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }


    private ResponseStatusException notFound() {

        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Không tìm thấy lịch sử biến động kho."
        );
    }
}