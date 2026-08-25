package org.example.datn_sd69.modules.goodsreceipt.service.impl;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.*;
import org.example.datn_sd69.enums.GoodsReceiptStatus;
import org.example.datn_sd69.enums.GoodsReceiptType;
import org.example.datn_sd69.modules.goodsreceipt.dto.request.*;
import org.example.datn_sd69.modules.goodsreceipt.dto.response.*;
import org.example.datn_sd69.modules.goodsreceipt.service.GoodsReceiptService;
import org.example.datn_sd69.repository.*;
import org.example.datn_sd69.repository.projection.GoodsReceiptListProjection;
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GoodsReceiptServiceImpl implements GoodsReceiptService {

    /*
     * Các SQL error code biểu thị state conflict/stale, không phải validation 400.
     * Giữ nguyên stored procedure hiện tại, chỉ map đúng HTTP status.
     */
    private static final Set<Integer> GOODS_RECEIPT_CONFLICT_SQL_CODES =
            Set.of(50011, 50022, 50031, 50033, 50041);

    private final GoodsReceiptRepository goodsReceiptRepository;
    private final GoodsReceiptItemRepository goodsReceiptItemRepository;
    private final GoodsReceiptApprovalHistoryRepository approvalHistoryRepository;

    // Repository này đã có sẵn trong module Product của project.
    private final ProductVariantRepository productVariantRepository;

    // Chỉ dùng để lấy ảnh đại diện sản phẩm cho response chi tiết phiếu nhập.
    private final ProductImageRepository productImageRepository;

    // Repository này đã có sẵn trong project và có findByEmailIgnoreCase(...).
    private final UserRepository userRepository;

    private final JdbcTemplate jdbcTemplate;
    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public Page<GoodsReceiptListResponse> getList(
            String keyword,
            GoodsReceiptStatus status,
            GoodsReceiptType receiptType,
            Integer createdBy,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable
    ) {
        if (
                fromDate != null &&
                        toDate != null &&
                        fromDate.isAfter(toDate)
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Từ ngày không được lớn hơn đến ngày."
            );
        }

        LocalDateTime from =
                fromDate == null
                        ? null
                        : fromDate.atStartOfDay();

        LocalDateTime toExclusive =
                toDate == null
                        ? null
                        : toDate.plusDays(1).atStartOfDay();

        Page<GoodsReceiptListProjection> page =
                goodsReceiptRepository.search(
                        normalizeOptional(keyword),
                        status == null
                                ? null
                                : status.getCode(),
                        receiptType == null
                                ? null
                                : receiptType.getCode(),
                        createdBy,
                        from,
                        toExclusive,
                        pageable
                );

        return page.map(this::mapList);
    }

    @Override
    @Transactional(readOnly = true)
    public GoodsReceiptDetailResponse getDetail(Integer id) {
        GoodsReceipt receipt = findReceipt(id);

        List<GoodsReceiptItem> items =
                goodsReceiptItemRepository.findByGoodsReceipt_IdOrderByIdAsc(id);

        List<GoodsReceiptItemResponse> itemResponses =
                items.stream().map(this::mapItem).toList();

        long totalSku = items.stream()
                .map(item -> item.getProductVariant().getId())
                .distinct()
                .count();

        long totalQuantity = items.stream()
                .mapToLong(item -> item.getQuantity() == null ? 0L : item.getQuantity().longValue())
                .sum();

        GoodsReceiptType type = GoodsReceiptType.fromCode(receipt.getReceiptType());
        GoodsReceiptStatus status = GoodsReceiptStatus.fromCode(receipt.getStatus());

        return GoodsReceiptDetailResponse.builder()
                .id(receipt.getId())
                .receiptNo(receipt.getReceiptNo())
                .receiptType(type)
                .receiptTypeLabel(type.getLabel())
                .status(status)
                .statusLabel(status.getLabel())
                .revision(buildRevision(receipt, items))
                .note(receipt.getNote())

                .createdById(userId(receipt.getCreatedBy()))
                .createdByName(userName(receipt.getCreatedBy()))
                .createdAt(receipt.getCreatedAt())

                .submittedById(userId(receipt.getSubmittedBy()))
                .submittedByName(userName(receipt.getSubmittedBy()))
                .submittedAt(receipt.getSubmittedAt())

                .approvedById(userId(receipt.getApprovedBy()))
                .approvedByName(userName(receipt.getApprovedBy()))
                .approvedAt(receipt.getApprovedAt())

                .rejectedById(userId(receipt.getRejectedBy()))
                .rejectedByName(userName(receipt.getRejectedBy()))
                .rejectedAt(receipt.getRejectedAt())
                .rejectionReason(receipt.getRejectionReason())

                .cancelledById(userId(receipt.getCancelledBy()))
                .cancelledByName(userName(receipt.getCancelledBy()))
                .cancelledAt(receipt.getCancelledAt())
                .cancellationReason(receipt.getCancellationReason())

                .totalSku(totalSku)
                .totalQuantity(totalQuantity)
                .items(itemResponses)
                .build();
    }

    @Override
    @Transactional
    public GoodsReceiptDetailResponse create(GoodsReceiptSaveRequest request) {
        User actor = getCurrentUser();

        LocalDateTime createdAt = LocalDateTime.now();
        LocalDate receivedDate = createdAt.toLocalDate();

        Map<Integer, ProductVariant> variants =
                validateAndLoadVariants(request.getItems(), receivedDate);

        GoodsReceipt receipt = new GoodsReceipt();
        receipt.setReceiptNo(generateReceiptNo());
        receipt.setReceiptType(request.getReceiptType().getCode());
        receipt.setStatus(GoodsReceiptStatus.DRAFT.getCode());
        receipt.setNote(normalizeOptional(request.getNote()));
        receipt.setCreatedBy(actor);
        receipt.setCreatedAt(createdAt);

        receipt = goodsReceiptRepository.saveAndFlush(receipt);

        saveItems(receipt, request.getItems(), variants, receivedDate);
        goodsReceiptItemRepository.flush();

        return getDetail(receipt.getId());
    }

    @Override
    @Transactional
    public GoodsReceiptDetailResponse update(Integer id, GoodsReceiptSaveRequest request) {
        /*
         * UPDATE là mutation duy nhất không đi qua stored procedure.
         * Khóa row trước khi kiểm tra state + revision để serialize:
         * - update <-> update
         * - update <-> submit
         * - update <-> cancel
         */
        GoodsReceipt receipt = findReceiptForUpdate(id);

        requireStatus(receipt, GoodsReceiptStatus.DRAFT,
                "Chỉ phiếu lưu tạm mới được sửa.");

        /*
         * Đọc đúng snapshot hiện tại trong DB sau khi đã giữ lock.
         * Nếu một tab khác đã sửa DRAFT trước đó thì revision sẽ khác,
         * dù Status vẫn còn là DRAFT.
         */
        List<GoodsReceiptItem> currentItems =
                goodsReceiptItemRepository.findByGoodsReceipt_IdOrderByIdAsc(id);

        validateExpectedRevision(
                request.getExpectedRevision(),
                buildRevision(receipt, currentItems)
        );

        GoodsReceiptType currentType =
                GoodsReceiptType.fromCode(receipt.getReceiptType());

        if (request.getReceiptType() != currentType) {
            throw badRequest("Không được thay đổi loại phiếu.");
        }

        LocalDate receivedDate =
                receipt.getCreatedAt() == null
                        ? LocalDate.now()
                        : receipt.getCreatedAt().toLocalDate();

        Map<Integer, ProductVariant> variants =
                validateAndLoadVariants(request.getItems(), receivedDate);

        receipt.setReceiptType(request.getReceiptType().getCode());
        receipt.setNote(normalizeOptional(request.getNote()));

        goodsReceiptRepository.saveAndFlush(receipt);

        goodsReceiptItemRepository.deleteAllByGoodsReceipt_Id(id);
        goodsReceiptItemRepository.flush();

        saveItems(receipt, request.getItems(), variants, receivedDate);
        goodsReceiptItemRepository.flush();

        return getDetail(id);
    }

    @Override
    @Transactional
    public GoodsReceiptDetailResponse submit(Integer id) {
        GoodsReceipt receipt = findReceipt(id);

        requireStatus(receipt, GoodsReceiptStatus.DRAFT,
                "Chỉ phiếu lưu tạm mới được gửi duyệt.");

        if (!goodsReceiptItemRepository.existsByGoodsReceipt_Id(id)) {
            throw badRequest("Phiếu nhập phải có ít nhất một sản phẩm.");
        }

        User actor = getCurrentUser();

        executeProcedure(
                "EXEC dbo.usp_GoodsReceipt_Submit ?, ?",
                id,
                actor.getId()
        );

        clearPersistenceContext();

        return getDetail(id);
    }

    @Override
    @Transactional
    public GoodsReceiptDetailResponse approve(Integer id) {
        GoodsReceipt receipt = findReceipt(id);

        requireStatus(receipt, GoodsReceiptStatus.PENDING_APPROVAL,
                "Chỉ phiếu đang chờ duyệt mới được phê duyệt.");

        User actor = getCurrentUser();

        executeProcedure(
                "EXEC dbo.usp_GoodsReceipt_Approve ?, ?",
                id,
                actor.getId()
        );

        clearPersistenceContext();

        return getDetail(id);
    }

    @Override
    @Transactional
    public GoodsReceiptDetailResponse reject(Integer id, GoodsReceiptRejectRequest request) {
        GoodsReceipt receipt = findReceipt(id);

        requireStatus(receipt, GoodsReceiptStatus.PENDING_APPROVAL,
                "Chỉ phiếu đang chờ duyệt mới được từ chối.");

        User actor = getCurrentUser();
        String reason = normalizeRequired(request.getReason(), "Bắt buộc nhập lý do từ chối.");

        executeProcedure(
                "EXEC dbo.usp_GoodsReceipt_Reject ?, ?, ?",
                id,
                actor.getId(),
                reason
        );

        clearPersistenceContext();

        return getDetail(id);
    }

    @Override
    @Transactional
    public GoodsReceiptDetailResponse cancel(Integer id, GoodsReceiptCancelRequest request) {
        GoodsReceipt receipt = findReceipt(id);

        requireStatus(receipt, GoodsReceiptStatus.DRAFT,
                "Chỉ phiếu lưu tạm mới được hủy.");

        User actor = getCurrentUser();
        String reason = request == null ? null : normalizeOptional(request.getReason());

        if (reason == null) {
            executeProcedure(
                    "EXEC dbo.usp_GoodsReceipt_Cancel ?, ?",
                    id,
                    actor.getId()
            );
        } else {
            executeProcedure(
                    "EXEC dbo.usp_GoodsReceipt_Cancel ?, ?, ?",
                    id,
                    actor.getId(),
                    reason
            );
        }

        clearPersistenceContext();

        return getDetail(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GoodsReceiptApprovalHistoryResponse> getApprovalHistory(Integer id) {
        findReceipt(id);

        return approvalHistoryRepository
                .findByGoodsReceipt_IdOrderByActionAtAscIdAsc(id)
                .stream()
                .map(this::mapHistory)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PendingReceiptCountResponse getPendingCount() {
        long count = goodsReceiptRepository.countByStatusAndReceiptType(
                GoodsReceiptStatus.PENDING_APPROVAL.getCode(),
                GoodsReceiptType.NORMAL_RECEIPT.getCode()
        );

        return new PendingReceiptCountResponse(count);
    }

    private Map<Integer, ProductVariant> validateAndLoadVariants(
            List<GoodsReceiptItemRequest> items,
            LocalDate receivedDate
    ) {
        if (items == null || items.isEmpty()) {
            throw badRequest("Phiếu nhập phải có ít nhất một sản phẩm.");
        }

        Set<Integer> variantIds = items.stream()
                .map(GoodsReceiptItemRequest::getProductVariantId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (variantIds.size() != items.size()) {
            throw badRequest(
                    "Mỗi SKU chỉ được chọn một lần trong cùng một phiếu nhập."
            );
        }

        List<ProductVariant> variants =
                productVariantRepository.findAvailableForGoodsReceiptByIds(variantIds);

        Map<Integer, ProductVariant> variantMap = variants.stream()
                .collect(Collectors.toMap(
                        ProductVariant::getId,
                        Function.identity()
                ));

        List<Integer> unavailableIds = variantIds.stream()
                .filter(id -> !variantMap.containsKey(id))
                .toList();

        if (!unavailableIds.isEmpty()) {
            throw badRequest(
                    "SKU không tồn tại hoặc thuộc sản phẩm/SKU đã bị xóa, "
                            + "không thể thêm vào phiếu nhập: "
                            + unavailableIds
            );
        }

        for (int i = 0; i < items.size(); i++) {
            GoodsReceiptItemRequest item = items.get(i);

            if (item.getProductVariantId() == null) {
                throw badRequest("Dòng " + (i + 1) + ": ProductVariantId không được để trống.");
            }

            if (item.getExpirationDate() == null) {
                throw badRequest("Dòng " + (i + 1) + ": hạn sử dụng không được để trống.");
            }

            if (item.getExpirationDate().isBefore(receivedDate)) {
                throw badRequest(
                        "Dòng " + (i + 1) +
                                ": hạn sử dụng phải lớn hơn hoặc bằng ngày nhận hàng."
                );
            }

            if (item.getManufacturedDate() != null
                    && item.getManufacturedDate().isAfter(receivedDate)) {
                throw badRequest(
                        "Dòng " + (i + 1) +
                                ": ngày sản xuất phải nhỏ hơn hoặc bằng ngày nhận hàng."
                );
            }

            if (item.getManufacturedDate() != null
                    && item.getManufacturedDate().isAfter(item.getExpirationDate())) {
                throw badRequest(
                        "Dòng " + (i + 1) +
                                ": ngày sản xuất phải nhỏ hơn hoặc bằng hạn sử dụng."
                );
            }
        }

        return variantMap;
    }

    private void saveItems(
            GoodsReceipt receipt,
            List<GoodsReceiptItemRequest> requests,
            Map<Integer, ProductVariant> variants,
            LocalDate receivedDate
    ) {
        List<GoodsReceiptItem> entities = new ArrayList<>();
        Set<String> generatedLotCodes = new HashSet<>();

        for (int i = 0; i < requests.size(); i++) {
            GoodsReceiptItemRequest request = requests.get(i);
            GoodsReceiptItem item = new GoodsReceiptItem();

            item.setGoodsReceipt(receipt);
            item.setProductVariant(variants.get(request.getProductVariantId()));
            item.setLotCode(generateLotCode(generatedLotCodes));
            item.setQuantity(request.getQuantity());
            item.setUnitCost(request.getUnitCost());
            item.setManufacturedDate(request.getManufacturedDate());
            item.setReceivedDate(receivedDate);
            item.setExpirationDate(request.getExpirationDate());
            item.setNote(normalizeOptional(request.getNote()));

            entities.add(item);
        }

        goodsReceiptItemRepository.saveAll(entities);
    }

    private GoodsReceiptListResponse mapList(GoodsReceiptListProjection projection) {
        GoodsReceiptType type = GoodsReceiptType.fromCode(projection.getReceiptType());
        GoodsReceiptStatus status = GoodsReceiptStatus.fromCode(projection.getStatus());

        return GoodsReceiptListResponse.builder()
                .id(projection.getId())
                .receiptNo(projection.getReceiptNo())
                .receiptType(type)
                .receiptTypeLabel(type.getLabel())
                .status(status)
                .statusLabel(status.getLabel())
                .note(projection.getNote())
                .createdById(projection.getCreatedById())
                .createdByName(projection.getCreatedByName())
                .createdAt(projection.getCreatedAt())
                .submittedAt(projection.getSubmittedAt())
                .approvedAt(projection.getApprovedAt())
                .rejectedAt(projection.getRejectedAt())
                .cancelledAt(projection.getCancelledAt())
                .totalSku(projection.getTotalSku() == null ? 0L : projection.getTotalSku())
                .totalQuantity(projection.getTotalQuantity() == null ? 0L : projection.getTotalQuantity())
                .build();
    }

    private GoodsReceiptItemResponse mapItem(GoodsReceiptItem item) {
        ProductVariant variant = item.getProductVariant();

        String productName = null;
        String imageUrl = null;

        if (variant != null && variant.getProduct() != null) {
            productName = variant.getProduct().getName();
            imageUrl = resolveProductImageUrl(variant);
        }

        return GoodsReceiptItemResponse.builder()
                .id(item.getId())
                .productVariantId(variant == null ? null : variant.getId())
                .sku(variant == null ? null : variant.getSku())
                .productName(productName)
                .imageUrl(imageUrl)
                .capacityValue(
                        variant != null && variant.getCapacity() != null
                                ? variant.getCapacity().getValue()
                                : null
                )
                .bottleTypeName(
                        variant != null && variant.getBottleType() != null
                                ? variant.getBottleType().getName()
                                : null
                )
                .lotCode(item.getLotCode())
                .quantity(item.getQuantity())
                .unitCost(item.getUnitCost())
                .manufacturedDate(item.getManufacturedDate())
                .receivedDate(item.getReceivedDate())
                .expirationDate(item.getExpirationDate())
                .note(item.getNote())
                .build();
    }

    private String resolveProductImageUrl(ProductVariant variant) {
        if (variant == null
                || variant.getProduct() == null
                || variant.getProduct().getId() == null) {
            return null;
        }

        Integer productId = variant.getProduct().getId();

        Optional<ProductImage> primaryImage =
                productImageRepository.findFirstByProduct_IdAndIsPrimaryTrue(productId);

        if (primaryImage.isPresent()) {
            return primaryImage.get().getImageUrl();
        }

        return productImageRepository
                .findFirstByProduct_Id(productId)
                .map(ProductImage::getImageUrl)
                .orElse(null);
    }

    private GoodsReceiptApprovalHistoryResponse mapHistory(
            GoodsReceiptApprovalHistory history
    ) {
        GoodsReceiptStatus fromStatus =
                GoodsReceiptStatus.fromCode(history.getFromStatus());

        GoodsReceiptStatus toStatus =
                GoodsReceiptStatus.fromCode(history.getToStatus());

        return GoodsReceiptApprovalHistoryResponse.builder()
                .id(history.getId())
                .fromStatus(fromStatus)
                .fromStatusLabel(fromStatus == null ? null : fromStatus.getLabel())
                .toStatus(toStatus)
                .toStatusLabel(toStatus == null ? null : toStatus.getLabel())
                .actionById(userId(history.getActionBy()))
                .actionByName(userName(history.getActionBy()))
                .reason(history.getReason())
                .actionAt(history.getActionAt())
                .build();
    }

    private GoodsReceipt findReceipt(Integer id) {
        if (id == null || id <= 0) {
            throw badRequest("Id phiếu nhập không hợp lệ.");
        }

        return goodsReceiptRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Không tìm thấy phiếu nhập."
                        )
                );
    }

    private GoodsReceipt findReceiptForUpdate(Integer id) {
        if (id == null || id <= 0) {
            throw badRequest("Id phiếu nhập không hợp lệ.");
        }

        return goodsReceiptRepository.findByIdForUpdate(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Không tìm thấy phiếu nhập."
                        )
                );
    }

    private void validateExpectedRevision(
            String expectedRevision,
            String currentRevision
    ) {
        String expected = normalizeOptional(expectedRevision);

        if (expected == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Dữ liệu phiếu nhập trên màn hình chưa có phiên bản hiện tại. "
                            + "Vui lòng tải lại phiếu và xác nhận lại."
            );
        }

        if (!expected.equals(currentRevision)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Phiếu nhập đã được thay đổi ở nơi khác. "
                            + "Vui lòng tải lại dữ liệu mới nhất và xác nhận lại."
            );
        }
    }

    private String buildRevision(
            GoodsReceipt receipt,
            List<GoodsReceiptItem> items
    ) {
        StringBuilder source = new StringBuilder();

        appendRevisionPart(source, receipt == null ? null : receipt.getId());
        appendRevisionPart(source, receipt == null ? null : receipt.getReceiptNo());
        appendRevisionPart(source, receipt == null ? null : receipt.getReceiptType());
        appendRevisionPart(source, receipt == null ? null : receipt.getStatus());
        appendRevisionPart(source, receipt == null ? null : receipt.getNote());
        appendRevisionPart(source, receipt == null ? null : receipt.getCreatedAt());

        if (items != null) {
            for (GoodsReceiptItem item : items) {
                ProductVariant variant = item.getProductVariant();

                appendRevisionPart(source, item.getId());
                appendRevisionPart(source, variant == null ? null : variant.getId());
                appendRevisionPart(source, item.getLotCode());
                appendRevisionPart(source, item.getQuantity());
                appendRevisionPart(
                        source,
                        item.getUnitCost() == null
                                ? null
                                : item.getUnitCost().toPlainString()
                );
                appendRevisionPart(source, item.getManufacturedDate());
                appendRevisionPart(source, item.getReceivedDate());
                appendRevisionPart(source, item.getExpirationDate());
                appendRevisionPart(source, item.getNote());
            }
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    source.toString().getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Không thể tạo phiên bản dữ liệu phiếu nhập.",
                    ex
            );
        }
    }

    private void appendRevisionPart(StringBuilder source, Object value) {
        String text = value == null ? "<NULL>" : String.valueOf(value);

        source.append(text.length())
                .append(':')
                .append(text)
                .append('|');
    }

    private void requireStatus(
            GoodsReceipt receipt,
            GoodsReceiptStatus expected,
            String message
    ) {
        GoodsReceiptStatus current =
                GoodsReceiptStatus.fromCode(receipt.getStatus());

        if (current != expected) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    message + " Trạng thái hiện tại: " + current.getLabel()
            );
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

    private String generateReceiptNo() {
        String date =
                LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("yyyyMMdd")
                );

        final String chars =
                "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

        for (int attempt = 0; attempt < 20; attempt++) {
            StringBuilder suffix = new StringBuilder(6);
            Random random = new Random();

            for (int i = 0; i < 6; i++) {
                suffix.append(
                        chars.charAt(
                                random.nextInt(chars.length())
                        )
                );
            }

            String receiptNo =
                    "PN-" + date + "-" + suffix;

            if (!goodsReceiptRepository.existsByReceiptNo(receiptNo)) {
                return receiptNo;
            }
        }

        throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Không thể sinh mã phiếu nhập."
        );
    }

    private String generateLotCode(Set<String> generatedLotCodes) {
        /*
         * Mã lô mới chỉ gồm đúng 6 ký tự A-F / 0-9.
         * Không đổi mã lô cũ đã tồn tại trong hệ thống.
         *
         * Kiểm tra cả:
         * - các mã vừa sinh trong cùng phiếu hiện tại;
         * - GoodsReceiptItem đã lưu;
         * - InventoryLot đã tạo.
         */
        for (int attempt = 0; attempt < 20; attempt++) {
            String lotCode = UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 6)
                    .toUpperCase(Locale.ROOT);

            if (generatedLotCodes.contains(lotCode)
                    || lotCodeExists(lotCode)) {
                continue;
            }

            generatedLotCodes.add(lotCode);
            return lotCode;
        }

        throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Không thể sinh mã lô 6 ký tự."
        );
    }

    private boolean lotCodeExists(String lotCode) {
        Integer exists = jdbcTemplate.queryForObject(
                """
                        SELECT CASE
                            WHEN EXISTS (
                                SELECT 1
                                FROM dbo.GoodsReceiptItem
                                WHERE LotCode = ?
                            )
                            OR EXISTS (
                                SELECT 1
                                FROM dbo.InventoryLot
                                WHERE LotCode = ?
                            )
                            THEN 1
                            ELSE 0
                        END
                        """,
                Integer.class,
                lotCode,
                lotCode
        );

        return exists != null && exists == 1;
    }

    private void executeProcedure(String sql, Object... args) {
        try {
            entityManager.flush();
            jdbcTemplate.update(sql, args);
        } catch (DataAccessException ex) {
            Throwable cause = ex.getMostSpecificCause();

            String message =
                    cause != null && cause.getMessage() != null
                            ? cause.getMessage()
                            : ex.getMessage();

            HttpStatus status =
                    isGoodsReceiptStateConflict(ex)
                            ? HttpStatus.CONFLICT
                            : HttpStatus.BAD_REQUEST;

            throw new ResponseStatusException(
                    status,
                    message == null
                            ? "Không thể xử lý nghiệp vụ phiếu nhập."
                            : message
            );
        }
    }

    private boolean isGoodsReceiptStateConflict(DataAccessException ex) {
        Throwable current = ex;

        while (current != null) {
            if (current instanceof SQLException sqlException
                    && GOODS_RECEIPT_CONFLICT_SQL_CODES.contains(
                    sqlException.getErrorCode()
            )) {
                return true;
            }

            current = current.getCause();
        }

        return false;
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
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }
}