package org.example.datn_sd69.modules.inventory.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.InventoryConfig;
import org.example.datn_sd69.enums.InventoryStockStatus;
import org.example.datn_sd69.modules.inventory.dto.InventoryConfigResponse;
import org.example.datn_sd69.modules.inventory.dto.InventoryConfigUpdateRequest;
import org.example.datn_sd69.modules.inventory.dto.InventoryLotStatusResponse;
import org.example.datn_sd69.modules.inventory.dto.InventorySummaryResponse;
import org.example.datn_sd69.modules.inventory.dto.response.InventoryOverviewResponse;
import org.example.datn_sd69.modules.inventory.service.InventoryService;
import org.example.datn_sd69.repository.InventoryConfigRepository;
import org.example.datn_sd69.repository.InventoryQueryRepository;
import org.example.datn_sd69.repository.projection.InventoryLotStatusProjection;
import org.example.datn_sd69.repository.projection.InventoryOverviewProjection;
import org.example.datn_sd69.repository.projection.InventorySummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl
        implements InventoryService {

    private static final byte CONFIG_ID = 1;

    private final InventoryQueryRepository inventoryQueryRepository;

    private final InventoryConfigRepository inventoryConfigRepository;


    /*
     * =========================================================
     * DASHBOARD SUMMARY
     * =========================================================
     */

    @Override
    @Transactional(readOnly = true)
    public InventorySummaryResponse getSummary() {

        InventorySummaryProjection projection =
                inventoryQueryRepository.getInventorySummary();

        if (projection == null) {
            return InventorySummaryResponse.builder()
                    .totalSku(0L)
                    .inStockSku(0L)
                    .outOfStockSku(0L)
                    .totalQuantity(0L)
                    .sellableQuantity(0L)
                    .nearExpiryQuantity(0L)
                    .expiredQuantity(0L)
                    .build();
        }

        return InventorySummaryResponse.builder()
                .totalSku(valueOrZero(projection.getTotalSku()))
                .inStockSku(valueOrZero(projection.getInStockSku()))
                .outOfStockSku(
                        valueOrZero(
                                projection.getOutOfStockSku()
                        )
                )
                .totalQuantity(
                        valueOrZero(
                                projection.getTotalQuantity()
                        )
                )
                .sellableQuantity(
                        valueOrZero(
                                projection.getSellableQuantity()
                        )
                )
                .nearExpiryQuantity(
                        valueOrZero(
                                projection.getNearExpiryQuantity()
                        )
                )
                .expiredQuantity(
                        valueOrZero(
                                projection.getExpiredQuantity()
                        )
                )
                .build();
    }


    /*
     * =========================================================
     * OVERVIEW
     * =========================================================
     */

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryOverviewResponse> getOverview(
            String keyword,
            Boolean nearExpiry,
            Boolean expired,
            InventoryStockStatus stockStatus,
            Pageable pageable
    ) {

        String normalizedKeyword =
                normalizeKeyword(keyword);

        Integer nearExpiryFlag =
                toFlag(nearExpiry);

        Integer expiredFlag =
                toFlag(expired);

        String stockStatusValue =
                stockStatus == null
                        ? InventoryStockStatus.ALL.name()
                        : stockStatus.name();

        return inventoryQueryRepository
                .findInventoryOverview(
                        normalizedKeyword,
                        nearExpiryFlag,
                        expiredFlag,
                        stockStatusValue,
                        pageable
                )
                .map(this::toOverviewResponse);
    }


    /*
     * =========================================================
     * NEAR EXPIRY
     * =========================================================
     */

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryLotStatusResponse> getNearExpiryLots(
            String keyword,
            Pageable pageable
    ) {

        return inventoryQueryRepository
                .findNearExpiryLots(
                        normalizeKeyword(keyword),
                        pageable
                )
                .map(this::toLotStatusResponse);
    }


    /*
     * =========================================================
     * EXPIRED
     * =========================================================
     */

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryLotStatusResponse> getExpiredLots(
            String keyword,
            Pageable pageable
    ) {

        return inventoryQueryRepository
                .findExpiredLots(
                        normalizeKeyword(keyword),
                        pageable
                )
                .map(this::toLotStatusResponse);
    }


    /*
     * =========================================================
     * GET CONFIG
     * =========================================================
     */

    @Override
    @Transactional(readOnly = true)
    public InventoryConfigResponse getConfig() {

        InventoryConfig config =
                getInventoryConfig();

        return toConfigResponse(config);
    }


    /*
     * =========================================================
     * UPDATE CONFIG
     * =========================================================
     */

    @Override
    @Transactional
    public InventoryConfigResponse updateConfig(
            InventoryConfigUpdateRequest request
    ) {

        /*
         * Chỉ mutation này dùng row lock.
         * Các API GET vẫn đọc như cũ, không bị khóa thêm.
         */
        InventoryConfig config =
                getInventoryConfigForUpdate();

        /*
         * expectedExpiryWarningDays chỉ là snapshot FE đang nhìn thấy.
         * Giá trị thật để ghi vẫn luôn là request.expiryWarningDays.
         */
        validateConfigSnapshot(request, config);

        config.setExpiryWarningDays(
                request.getExpiryWarningDays()
        );

        inventoryConfigRepository.save(config);

        return toConfigResponse(config);
    }


    /*
     * =========================================================
     * MAPPER
     * =========================================================
     */

    private InventoryOverviewResponse toOverviewResponse(
            InventoryOverviewProjection projection
    ) {

        return InventoryOverviewResponse.builder()
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
                .totalQuantity(
                        valueOrZero(
                                projection.getTotalQuantity()
                        )
                )
                .sellableQuantity(
                        valueOrZero(
                                projection.getSellableQuantity()
                        )
                )
                .nearExpiryQuantity(
                        valueOrZero(
                                projection.getNearExpiryQuantity()
                        )
                )
                .expiredQuantity(
                        valueOrZero(
                                projection.getExpiredQuantity()
                        )
                )
                .build();
    }


    private InventoryLotStatusResponse toLotStatusResponse(
            InventoryLotStatusProjection projection
    ) {

        return InventoryLotStatusResponse.builder()
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
                .manufacturedDate(
                        projection.getManufacturedDate()
                )
                .receivedDate(
                        projection.getReceivedDate()
                )
                .expirationDate(
                        projection.getExpirationDate()
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
                .daysToExpiry(
                        projection.getDaysToExpiry()
                )
                .nearExpiry(
                        Boolean.TRUE.equals(
                                projection.getNearExpiry()
                        )
                )
                .expired(
                        Boolean.TRUE.equals(
                                projection.getExpired()
                        )
                )
                .build();
    }


    private InventoryConfigResponse toConfigResponse(
            InventoryConfig config
    ) {

        return InventoryConfigResponse.builder()
                .id(config.getId())
                .expiryWarningDays(
                        config.getExpiryWarningDays()
                )
                .build();
    }


    /*
     * =========================================================
     * HELPERS
     * =========================================================
     */

    private InventoryConfig getInventoryConfig() {

        return inventoryConfigRepository
                .findById(CONFIG_ID)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Không tìm thấy cấu hình kho"
                        )
                );
    }


    /**
     * Chỉ dùng cho UPDATE CONFIG.
     *
     * PESSIMISTIC_WRITE đảm bảo:
     * - Request A khóa row và cập nhật trước.
     * - Request B phải chờ.
     * - Sau khi A commit, B đọc được state mới rồi mới so expected snapshot.
     * - Nếu B vẫn gửi snapshot cũ thì trả 409, không ghi đè A.
     */
    private InventoryConfig getInventoryConfigForUpdate() {

        return inventoryConfigRepository
                .findByIdForUpdate(CONFIG_ID)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Không tìm thấy cấu hình kho"
                        )
                );
    }


    private void validateConfigSnapshot(
            InventoryConfigUpdateRequest request,
            InventoryConfig currentConfig
    ) {

        if (request == null || currentConfig == null) {
            return;
        }

        Short expectedExpiryWarningDays =
                request.getExpectedExpiryWarningDays();

        /*
         * Nullable để không làm vỡ caller cũ.
         * Khi FE mới gửi expected* thì stale-check mới được kích hoạt.
         */
        if (expectedExpiryWarningDays == null) {
            return;
        }

        Short currentExpiryWarningDays =
                currentConfig.getExpiryWarningDays();

        if (!Objects.equals(
                expectedExpiryWarningDays,
                currentExpiryWarningDays
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cấu hình cảnh báo hạn sử dụng đã thay đổi từ "
                            + expectedExpiryWarningDays
                            + " ngày thành "
                            + currentExpiryWarningDays
                            + " ngày. Vui lòng cập nhật lại dữ liệu và xác nhận lại."
            );
        }
    }


    private String normalizeKeyword(String keyword) {

        if (keyword == null) {
            return null;
        }

        String value = keyword.trim();

        return value.isEmpty()
                ? null
                : value;
    }


    private Integer toFlag(Boolean value) {

        if (value == null) {
            return null;
        }

        return value ? 1 : 0;
    }


    private Long valueOrZero(Long value) {

        return value == null ? 0L : value;
    }
}