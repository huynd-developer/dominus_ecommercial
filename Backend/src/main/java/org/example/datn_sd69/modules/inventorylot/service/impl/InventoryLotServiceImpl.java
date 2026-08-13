package org.example.datn_sd69.modules.inventorylot.service.impl;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.InventoryLot;
import org.example.datn_sd69.entity.InventoryLotLockHistory;
import org.example.datn_sd69.entity.User;
import org.example.datn_sd69.enums.GoodsReceiptStatus;
import org.example.datn_sd69.enums.GoodsReceiptType;
import org.example.datn_sd69.enums.InventoryLotLockActionType;
import org.example.datn_sd69.modules.inventorylot.dto.request.InventoryLotLockRequest;
import org.example.datn_sd69.modules.inventorylot.dto.request.InventoryLotUnlockRequest;
import org.example.datn_sd69.modules.inventorylot.dto.response.*;
import org.example.datn_sd69.modules.inventorylot.service.InventoryLotService;
import org.example.datn_sd69.repository.InventoryLotLockHistoryRepository;
import org.example.datn_sd69.repository.InventoryLotRepository;
import org.example.datn_sd69.repository.UserRepository;
import org.example.datn_sd69.repository.projection.InventoryLotViewProjection;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryLotServiceImpl implements InventoryLotService {

    private final InventoryLotRepository inventoryLotRepository;
    private final InventoryLotLockHistoryRepository lockHistoryRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryLotListResponse> getList(
            String keyword,
            Integer productVariantId,
            Boolean isLocked,
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
            throw badRequest("Từ ngày HSD không được lớn hơn đến ngày HSD.");
        }

        return inventoryLotRepository.search(
                normalizeOptional(keyword),
                productVariantId,
                isLocked,
                isExpired,
                isNearExpiry,
                hasStock,
                expirationFrom,
                expirationTo,
                pageable
        ).map(this::mapList);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryLotDetailResponse getDetail(Integer id) {
        InventoryLotViewProjection projection = findView(id);
        return mapDetail(projection);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryLotSourceResponse getSource(Integer id) {
        InventoryLotViewProjection projection = findView(id);

        GoodsReceiptType receiptType = GoodsReceiptType.fromCode(projection.getReceiptType());
        GoodsReceiptStatus receiptStatus = GoodsReceiptStatus.fromCode(projection.getReceiptStatus());

        return InventoryLotSourceResponse.builder()
                .inventoryLotId(projection.getId())
                .goodsReceiptItemId(projection.getGoodsReceiptItemId())
                .goodsReceiptId(projection.getGoodsReceiptId())
                .receiptNo(projection.getReceiptNo())
                .receiptType(receiptType)
                .receiptTypeLabel(receiptType == null ? null : receiptType.getLabel())
                .receiptStatus(receiptStatus)
                .receiptStatusLabel(receiptStatus == null ? null : receiptStatus.getLabel())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryLotLockHistoryResponse> getLockHistory(Integer id) {
        validateId(id);

        if (!inventoryLotRepository.existsById(id)) {
            throw notFound();
        }

        return lockHistoryRepository
                .findByInventoryLot_IdOrderByActionAtDescIdDesc(id)
                .stream()
                .map(this::mapHistory)
                .toList();
    }

    @Override
    @Transactional
    public InventoryLotDetailResponse lock(
            Integer id,
            InventoryLotLockRequest request
    ) {
        validateId(id);

        InventoryLot lot = findEntity(id);

        if (Boolean.TRUE.equals(lot.getIsLocked())) {
            throw conflict("Lô hiện đang bị khóa.");
        }

        String reason = normalizeRequired(
                request == null ? null : request.getReason(),
                "Bắt buộc nhập lý do khóa lô."
        );

        User actor = getCurrentUser();

        executeProcedure(
                "EXEC dbo.usp_InventoryLot_Lock ?, ?, ?",
                id,
                actor.getId(),
                reason
        );

        clearPersistenceContext();

        return getDetail(id);
    }

    @Override
    @Transactional
    public InventoryLotDetailResponse unlock(
            Integer id,
            InventoryLotUnlockRequest request
    ) {
        validateId(id);

        InventoryLot lot = findEntity(id);

        if (!Boolean.TRUE.equals(lot.getIsLocked())) {
            throw conflict("Lô hiện không bị khóa.");
        }

        String reason = request == null
                ? null
                : normalizeOptional(request.getReason());

        User actor = getCurrentUser();

        if (reason == null) {
            executeProcedure(
                    "EXEC dbo.usp_InventoryLot_Unlock ?, ?",
                    id,
                    actor.getId()
            );
        } else {
            executeProcedure(
                    "EXEC dbo.usp_InventoryLot_Unlock ?, ?, ?",
                    id,
                    actor.getId(),
                    reason
            );
        }

        clearPersistenceContext();

        return getDetail(id);
    }

    private InventoryLotListResponse mapList(InventoryLotViewProjection projection) {
        return InventoryLotListResponse.builder()
                .id(projection.getId())
                .productVariantId(projection.getProductVariantId())
                .sku(projection.getSku())
                .productName(projection.getProductName())
                .lotCode(projection.getLotCode())
                .receivedDate(projection.getReceivedDate())
                .expirationDate(projection.getExpirationDate())
                .daysToExpiry(projection.getDaysToExpiry())
                .initialQuantity(projection.getInitialQuantity())
                .quantityOnHand(projection.getQuantityOnHand())
                .sellableQuantity(projection.getSellableQuantity())
                .isNearExpiry(projection.getIsNearExpiry())
                .isExpired(projection.getIsExpired())
                .isLocked(projection.getIsLocked())
                .lockReason(projection.getLockReason())
                .goodsReceiptId(projection.getGoodsReceiptId())
                .receiptNo(projection.getReceiptNo())
                .build();
    }

    private InventoryLotDetailResponse mapDetail(InventoryLotViewProjection projection) {
        GoodsReceiptType receiptType = GoodsReceiptType.fromCode(projection.getReceiptType());
        GoodsReceiptStatus receiptStatus = GoodsReceiptStatus.fromCode(projection.getReceiptStatus());

        return InventoryLotDetailResponse.builder()
                .id(projection.getId())
                .productVariantId(projection.getProductVariantId())
                .sku(projection.getSku())
                .productName(projection.getProductName())
                .lotCode(projection.getLotCode())
                .manufacturedDate(projection.getManufacturedDate())
                .receivedDate(projection.getReceivedDate())
                .expirationDate(projection.getExpirationDate())
                .daysToExpiry(projection.getDaysToExpiry())
                .initialQuantity(projection.getInitialQuantity())
                .quantityOnHand(projection.getQuantityOnHand())
                .sellableQuantity(projection.getSellableQuantity())
                .isNearExpiry(projection.getIsNearExpiry())
                .isExpired(projection.getIsExpired())
                .isLocked(projection.getIsLocked())
                .lockReason(projection.getLockReason())
                .lockedById(projection.getLockedById())
                .lockedByName(projection.getLockedByName())
                .lockedAt(projection.getLockedAt())
                .createdById(projection.getCreatedById())
                .createdByName(projection.getCreatedByName())
                .createdAt(projection.getCreatedAt())
                .goodsReceiptItemId(projection.getGoodsReceiptItemId())
                .goodsReceiptId(projection.getGoodsReceiptId())
                .receiptNo(projection.getReceiptNo())
                .receiptType(receiptType)
                .receiptTypeLabel(receiptType == null ? null : receiptType.getLabel())
                .receiptStatus(receiptStatus)
                .receiptStatusLabel(receiptStatus == null ? null : receiptStatus.getLabel())
                .build();
    }

    private InventoryLotLockHistoryResponse mapHistory(
            InventoryLotLockHistory history
    ) {
        InventoryLotLockActionType actionType =
                InventoryLotLockActionType.fromCode(history.getActionType());

        return InventoryLotLockHistoryResponse.builder()
                .id(history.getId())
                .actionType(actionType)
                .actionTypeLabel(actionType == null ? null : actionType.getLabel())
                .reason(history.getReason())
                .actionById(userId(history.getActionBy()))
                .actionByName(userName(history.getActionBy()))
                .actionAt(history.getActionAt())
                .build();
    }

    private InventoryLotViewProjection findView(Integer id) {
        validateId(id);

        return inventoryLotRepository.findViewById(id)
                .orElseThrow(this::notFound);
    }

    private InventoryLot findEntity(Integer id) {
        return inventoryLotRepository.findById(id)
                .orElseThrow(this::notFound);
    }

    private void validateId(Integer id) {
        if (id == null || id <= 0) {
            throw badRequest("Id lô hàng không hợp lệ.");
        }
    }

    private User getCurrentUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Không xác định được người dùng hiện tại."
            );
        }

        return userRepository
                .findByEmailIgnoreCase(authentication.getName().trim())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Không tìm thấy tài khoản đăng nhập trong hệ thống."
                        )
                );
    }

    private void executeProcedure(String sql, Object... args) {
        try {
            entityManager.flush();
            jdbcTemplate.update(sql, args);
        } catch (DataAccessException ex) {
            Throwable cause = ex.getMostSpecificCause();

            String message = cause != null && cause.getMessage() != null
                    ? cause.getMessage()
                    : ex.getMessage();

            if (message != null
                    && (message.contains("đang bị khóa")
                    || message.contains("không bị khóa"))) {
                throw conflict(message);
            }

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    message == null
                            ? "Không thể xử lý nghiệp vụ lô hàng."
                            : message
            );
        }
    }

    private void clearPersistenceContext() {
        entityManager.clear();
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalizeOptional(value);

        if (normalized == null) {
            throw badRequest(message);
        }

        return normalized;
    }

    private Integer userId(User user) {
        return user == null ? null : user.getId();
    }

    private String userName(User user) {
        return user == null ? null : user.getName();
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Không tìm thấy lô hàng."
        );
    }
}
