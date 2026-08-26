package org.example.datn_sd69.modules.inventorylot.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.enums.GoodsReceiptStatus;
import org.example.datn_sd69.enums.GoodsReceiptType;
import org.example.datn_sd69.modules.inventorylot.dto.response.*;
import org.example.datn_sd69.modules.inventorylot.service.InventoryLotService;
import org.example.datn_sd69.repository.InventoryLotRepository;
import org.example.datn_sd69.repository.projection.InventoryLotViewProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class InventoryLotServiceImpl implements InventoryLotService {

    private final InventoryLotRepository inventoryLotRepository;

    // =========================================================
    // GENERIC LIST
    // =========================================================

    /**
     * Danh sách lô dùng chung.
     *
     * GIỮ NGUYÊN logic cũ để không ảnh hưởng các màn/module
     * đang sử dụng API danh sách InventoryLot hiện tại.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<InventoryLotListResponse> getList(
            String keyword,
            Integer productVariantId,
            Boolean isExpired,
            Boolean isNearExpiry,
            Boolean hasStock,
            LocalDate expirationFrom,
            LocalDate expirationTo,
            Pageable pageable
    ) {
        if (productVariantId != null && productVariantId <= 0) {
            throw badRequest("ProductVariantId không hợp lệ.");
        }

        if (expirationFrom != null
                && expirationTo != null
                && expirationFrom.isAfter(expirationTo)) {

            throw badRequest(
                    "Từ ngày HSD không được lớn hơn đến ngày HSD."
            );
        }

        return inventoryLotRepository.search(
                normalizeOptional(keyword),
                productVariantId,
                isExpired,
                isNearExpiry,
                hasStock,
                expirationFrom,
                expirationTo,
                pageable
        ).map(this::mapList);
    }

    // =========================================================
    // STOCK ADJUSTMENT CANDIDATES
    // =========================================================

    /**
     * Danh sách lô dành RIÊNG cho việc tạo phiếu kiểm kê.
     *
     * Không dùng generic search() để tránh thay đổi hành vi
     * của màn quản lý lô và các module khác.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<InventoryLotListResponse> getAuditCandidates(
            String keyword,
            Integer productVariantId,
            Boolean isExpired,
            Boolean isNearExpiry,
            Boolean hasStock,
            LocalDate expirationFrom,
            LocalDate expirationTo,
            Pageable pageable
    ) {
        if (productVariantId != null && productVariantId <= 0) {
            throw badRequest("ProductVariantId không hợp lệ.");
        }

        if (expirationFrom != null
                && expirationTo != null
                && expirationFrom.isAfter(expirationTo)) {

            throw badRequest(
                    "Từ ngày HSD không được lớn hơn đến ngày HSD."
            );
        }

        return inventoryLotRepository.searchAuditCandidates(
                normalizeOptional(keyword),
                productVariantId,
                isExpired,
                isNearExpiry,
                hasStock,
                expirationFrom,
                expirationTo,
                pageable
        ).map(this::mapList);
    }

    // =========================================================
    // DETAIL
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public InventoryLotDetailResponse getDetail(Integer id) {

        InventoryLotViewProjection projection =
                findView(id);

        return mapDetail(projection);
    }

    // =========================================================
    // SOURCE
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public InventoryLotSourceResponse getSource(Integer id) {

        InventoryLotViewProjection projection =
                findView(id);

        GoodsReceiptType receiptType =
                GoodsReceiptType.fromCode(
                        projection.getReceiptType()
                );

        GoodsReceiptStatus receiptStatus =
                GoodsReceiptStatus.fromCode(
                        projection.getReceiptStatus()
                );

        return InventoryLotSourceResponse.builder()
                .inventoryLotId(
                        projection.getId()
                )
                .goodsReceiptItemId(
                        projection.getGoodsReceiptItemId()
                )
                .goodsReceiptId(
                        projection.getGoodsReceiptId()
                )
                .receiptNo(
                        projection.getReceiptNo()
                )
                .receiptType(
                        receiptType
                )
                .receiptTypeLabel(
                        receiptType == null
                                ? null
                                : receiptType.getLabel()
                )
                .receiptStatus(
                        receiptStatus
                )
                .receiptStatusLabel(
                        receiptStatus == null
                                ? null
                                : receiptStatus.getLabel()
                )
                .build();
    }

    // =========================================================
    // MAPPING
    // =========================================================

    private InventoryLotListResponse mapList(
            InventoryLotViewProjection projection
    ) {
        return InventoryLotListResponse.builder()
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
                .receivedDate(
                        projection.getReceivedDate()
                )
                .expirationDate(
                        projection.getExpirationDate()
                )
                .daysToExpiry(
                        projection.getDaysToExpiry()
                )
                .initialQuantity(
                        projection.getInitialQuantity()
                )
                .quantityOnHand(
                        projection.getQuantityOnHand()
                )
                .sellableQuantity(
                        projection.getSellableQuantity()
                )
                .isNearExpiry(
                        projection.getIsNearExpiry()
                )
                .isExpired(
                        projection.getIsExpired()
                )
                .goodsReceiptId(
                        projection.getGoodsReceiptId()
                )
                .receiptNo(
                        projection.getReceiptNo()
                )
                .build();
    }

    private InventoryLotDetailResponse mapDetail(
            InventoryLotViewProjection projection
    ) {
        GoodsReceiptType receiptType =
                GoodsReceiptType.fromCode(
                        projection.getReceiptType()
                );

        GoodsReceiptStatus receiptStatus =
                GoodsReceiptStatus.fromCode(
                        projection.getReceiptStatus()
                );

        return InventoryLotDetailResponse.builder()
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
                .manufacturedDate(
                        projection.getManufacturedDate()
                )
                .receivedDate(
                        projection.getReceivedDate()
                )
                .expirationDate(
                        projection.getExpirationDate()
                )
                .daysToExpiry(
                        projection.getDaysToExpiry()
                )
                .initialQuantity(
                        projection.getInitialQuantity()
                )
                .quantityOnHand(
                        projection.getQuantityOnHand()
                )
                .sellableQuantity(
                        projection.getSellableQuantity()
                )
                .unitCost(
                        projection.getUnitCost()
                )
                .isNearExpiry(
                        projection.getIsNearExpiry()
                )
                .isExpired(
                        projection.getIsExpired()
                )
                .createdById(
                        projection.getCreatedById()
                )
                .createdByName(
                        projection.getCreatedByName()
                )
                .createdAt(
                        projection.getCreatedAt()
                )
                .goodsReceiptItemId(
                        projection.getGoodsReceiptItemId()
                )
                .goodsReceiptId(
                        projection.getGoodsReceiptId()
                )
                .receiptNo(
                        projection.getReceiptNo()
                )
                .receiptType(
                        receiptType
                )
                .receiptTypeLabel(
                        receiptType == null
                                ? null
                                : receiptType.getLabel()
                )
                .receiptStatus(
                        receiptStatus
                )
                .receiptStatusLabel(
                        receiptStatus == null
                                ? null
                                : receiptStatus.getLabel()
                )
                .build();
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private InventoryLotViewProjection findView(Integer id) {

        validateId(id);

        return inventoryLotRepository
                .findViewById(id)
                .orElseThrow(this::notFound);
    }

    private void validateId(Integer id) {

        if (id == null || id <= 0) {
            throw badRequest(
                    "Id lô hàng không hợp lệ."
            );
        }
    }

    private String normalizeOptional(String value) {

        if (value == null) {
            return null;
        }

        String normalized =
                value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
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
                "Không tìm thấy lô hàng."
        );
    }
}