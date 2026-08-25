package org.example.datn_sd69.modules.stockadjustment.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.InventoryLot;
import org.example.datn_sd69.entity.StockAdjustment;
import org.example.datn_sd69.entity.StockAdjustmentItem;
import org.example.datn_sd69.entity.User;
import org.example.datn_sd69.enums.StockAdjustmentStatus;
import org.example.datn_sd69.modules.stockadjustment.dto.request.StockAdjustmentCancelRequest;
import org.example.datn_sd69.modules.stockadjustment.dto.request.StockAdjustmentItemRequest;
import org.example.datn_sd69.modules.stockadjustment.dto.request.StockAdjustmentRejectRequest;
import org.example.datn_sd69.modules.stockadjustment.dto.request.StockAdjustmentSaveRequest;
import org.example.datn_sd69.modules.stockadjustment.dto.response.StockAdjustmentDetailResponse;
import org.example.datn_sd69.modules.stockadjustment.dto.response.StockAdjustmentItemResponse;
import org.example.datn_sd69.modules.stockadjustment.dto.response.StockAdjustmentListResponse;
import org.example.datn_sd69.modules.stockadjustment.service.StockAdjustmentService;
import org.example.datn_sd69.modules.stockadjustment.specification.StockAdjustmentSpecification;
import org.example.datn_sd69.repository.StockAdjustmentRepository;
import org.springframework.jdbc.core.PreparedStatementCallback;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StockAdjustmentServiceImpl
        implements StockAdjustmentService {

    private static final String REFERENCE_TYPE =
            "STOCK_ADJUSTMENT";

    private static final byte MOVEMENT_ADJUST_IN = 5;
    private static final byte MOVEMENT_ADJUST_OUT = 6;

    private final StockAdjustmentRepository stockAdjustmentRepository;

    private final EntityManager entityManager;

    private final JdbcTemplate jdbcTemplate;

    // =========================================================
    // LIST
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public Page<StockAdjustmentListResponse> getList(
            String keyword,
            StockAdjustmentStatus status,
            Integer createdBy,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable
    ) {

        validateDateRange(fromDate, toDate);

        if (createdBy != null && createdBy <= 0) {
            throw badRequest(
                    "createdBy phải lớn hơn 0."
            );
        }

        return stockAdjustmentRepository
                .findAll(
                        StockAdjustmentSpecification.build(
                                normalizeOptional(keyword),
                                status,
                                createdBy,
                                fromDate,
                                toDate
                        ),
                        pageable
                )
                .map(this::toListResponse);
    }

    // =========================================================
    // DETAIL
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public StockAdjustmentDetailResponse getDetail(
            Integer id
    ) {

        return toDetailResponse(
                requireAdjustment(id)
        );
    }

    // =========================================================
    // CREATE DRAFT
    // =========================================================

    @Override
    @Transactional
    public StockAdjustmentDetailResponse create(
            StockAdjustmentSaveRequest request
    ) {

        validateRequest(request);

        User currentUser =
                getCurrentUser();

        StockAdjustment adjustment =
                new StockAdjustment();

        adjustment.setAdjustmentNo(
                generateAdjustmentNo()
        );

        adjustment.setStatus(
                StockAdjustmentStatus.DRAFT.getCode()
        );

        adjustment.setNote(
                normalizeOptional(request.getNote())
        );

        adjustment.setCreatedBy(currentUser);
        adjustment.setCreatedAt(LocalDateTime.now());

        buildItems(
                adjustment,
                request.getItems()
        );

        StockAdjustment saved =
                stockAdjustmentRepository
                        .saveAndFlush(adjustment);

        return toDetailResponse(saved);
    }

    // =========================================================
    // UPDATE DRAFT
    // =========================================================

    @Override
    @Transactional
    public StockAdjustmentDetailResponse update(
            Integer id,
            StockAdjustmentSaveRequest request
    ) {

        validateRequest(request);

        StockAdjustment adjustment =
                requireAdjustmentForUpdate(id);

        requireStatus(
                adjustment,
                StockAdjustmentStatus.DRAFT,
                "Chỉ phiếu Lưu tạm mới được sửa."
        );

        User currentUser =
                getCurrentUser();

        ensureDraftOwnerOrOwner(
                adjustment,
                currentUser,
                "Chỉ người tạo phiếu hoặc OWNER được sửa phiếu Lưu tạm."
        );

        /*
         * Row StockAdjustment đã được khóa bởi findByIdForUpdate().
         * So sánh snapshot trước khi thay đổi note/items để chống:
         * A và B cùng mở DRAFT, B lưu trước, A lưu màn hình cũ sau.
         */
        validateExpectedRevision(
                request.getExpectedRevision(),
                buildRevision(adjustment)
        );

        adjustment.setNote(
                normalizeOptional(request.getNote())
        );

        /*
         * Xóa item cũ trước để tránh unique:
         * StockAdjustmentId + InventoryLotId
         */
        adjustment.clearItems();

        stockAdjustmentRepository.saveAndFlush(
                adjustment
        );

        buildItems(
                adjustment,
                request.getItems()
        );

        StockAdjustment saved =
                stockAdjustmentRepository
                        .saveAndFlush(adjustment);

        return toDetailResponse(saved);
    }

    // =========================================================
    // SUBMIT
    // =========================================================

    @Override
    @Transactional
    public StockAdjustmentDetailResponse submit(
            Integer id
    ) {

        StockAdjustment adjustment =
                requireAdjustmentForUpdate(id);

        requireStatus(
                adjustment,
                StockAdjustmentStatus.DRAFT,
                "Chỉ phiếu Lưu tạm mới được gửi duyệt."
        );

        if (adjustment.getItems() == null
                || adjustment.getItems().isEmpty()) {

            throw badRequest(
                    "Phiếu kiểm kê phải có ít nhất một lô hàng."
            );
        }

        User currentUser =
                getCurrentUser();

        ensureDraftOwnerOrOwner(
                adjustment,
                currentUser,
                "Chỉ người tạo phiếu hoặc OWNER được gửi duyệt."
        );

        adjustment.setStatus(
                StockAdjustmentStatus
                        .PENDING_APPROVAL
                        .getCode()
        );

        adjustment.setSubmittedBy(currentUser);
        adjustment.setSubmittedAt(LocalDateTime.now());

        stockAdjustmentRepository.saveAndFlush(
                adjustment
        );

        return toDetailResponse(adjustment);
    }

    // =========================================================
    // CANCEL DRAFT
    // =========================================================

    @Override
    @Transactional
    public StockAdjustmentDetailResponse cancel(
            Integer id,
            StockAdjustmentCancelRequest request
    ) {

        if (request == null) {
            throw badRequest(
                    "Dữ liệu hủy phiếu không được để trống."
            );
        }

        String reason =
                normalizeRequired(
                        request.getReason(),
                        "Lý do hủy không được để trống."
                );

        if (reason.length() > 500) {
            throw badRequest(
                    "Lý do hủy không được vượt quá 500 ký tự."
            );
        }

        StockAdjustment adjustment =
                requireAdjustmentForUpdate(id);

        requireStatus(
                adjustment,
                StockAdjustmentStatus.DRAFT,
                "Chỉ phiếu Lưu tạm mới được hủy."
        );

        User currentUser =
                getCurrentUser();

        ensureDraftOwnerOrOwner(
                adjustment,
                currentUser,
                "Chỉ người tạo phiếu hoặc OWNER được hủy phiếu Lưu tạm."
        );

        adjustment.setStatus(
                StockAdjustmentStatus
                        .CANCELLED
                        .getCode()
        );

        adjustment.setCancelledBy(currentUser);
        adjustment.setCancelledAt(LocalDateTime.now());
        adjustment.setCancellationReason(reason);

        stockAdjustmentRepository.saveAndFlush(
                adjustment
        );

        /*
         * Hủy phiếu Lưu tạm chỉ đổi trạng thái.
         * Không tạo StockMovement và không thay đổi InventoryLot.
         */
        return toDetailResponse(adjustment);
    }

    // =========================================================
    // APPROVE
    // =========================================================

    @Override
    @Transactional
    public StockAdjustmentDetailResponse approve(
            Integer id
    ) {

        StockAdjustment adjustment =
                requireAdjustmentForUpdate(id);

        requireStatus(
                adjustment,
                StockAdjustmentStatus.PENDING_APPROVAL,
                "Chỉ phiếu đang Chờ duyệt mới được phê duyệt."
        );

        User currentUser =
                getCurrentUser();

        ensureReviewerPermission(
                adjustment,
                currentUser
        );

        /*
         * Difference KHÔNG lưu DB.
         *
         * difference =
         * ActualQuantity - SystemQuantity
         *
         * Khi approve:
         * - difference > 0 => ADJUST_IN
         * - difference < 0 => ADJUST_OUT
         * - difference = 0 => không tạo movement
         *
         * Stored procedure tự lấy QuantityOnHand HIỆN TẠI,
         * sau đó áp difference lên tồn hiện tại.
         */
        for (StockAdjustmentItem item
                : adjustment.getItems()) {

            int difference =
                    calculateDifference(item);

            if (difference == 0) {
                continue;
            }

            String reason =
                    normalizeOptional(
                            item.getReason()
                    );

            if (reason == null) {
                throw badRequest(
                        "Lô "
                                + item.getInventoryLot()
                                .getLotCode()
                                + " có chênh lệch "
                                + signed(difference)
                                + " nên bắt buộc nhập lý do."
                );
            }

            byte movementType =
                    difference > 0
                            ? MOVEMENT_ADJUST_IN
                            : MOVEMENT_ADJUST_OUT;

            postStockMovement(
                    item.getInventoryLot().getId(),
                    movementType,
                    difference,
                    currentUser.getId(),
                    adjustment.getId(),
                    item.getId(),
                    reason,
                    safeInt(item.getSystemQuantity())
            );
        }

        adjustment.setStatus(
                StockAdjustmentStatus
                        .APPROVED
                        .getCode()
        );

        adjustment.setApprovedBy(currentUser);
        adjustment.setApprovedAt(LocalDateTime.now());

        stockAdjustmentRepository.saveAndFlush(
                adjustment
        );

        /*
         * Stored procedure UPDATE InventoryLot trực tiếp.
         * Clear persistence context để response đọc tồn mới.
         */
        entityManager.clear();

        return toDetailResponse(
                requireAdjustment(id)
        );
    }

    // =========================================================
    // REJECT
    // =========================================================

    @Override
    @Transactional
    public StockAdjustmentDetailResponse reject(
            Integer id,
            StockAdjustmentRejectRequest request
    ) {

        if (request == null) {
            throw badRequest(
                    "Dữ liệu từ chối không được để trống."
            );
        }

        String reason =
                normalizeRequired(
                        request.getReason(),
                        "Lý do từ chối không được để trống."
                );

        if (reason.length() > 500) {
            throw badRequest(
                    "Lý do từ chối không được vượt quá 500 ký tự."
            );
        }

        StockAdjustment adjustment =
                requireAdjustmentForUpdate(id);

        requireStatus(
                adjustment,
                StockAdjustmentStatus.PENDING_APPROVAL,
                "Chỉ phiếu đang Chờ duyệt mới được từ chối."
        );

        User currentUser =
                getCurrentUser();

        ensureReviewerPermission(
                adjustment,
                currentUser
        );

        adjustment.setStatus(
                StockAdjustmentStatus
                        .REJECTED
                        .getCode()
        );

        adjustment.setRejectedBy(currentUser);
        adjustment.setRejectedAt(LocalDateTime.now());
        adjustment.setRejectionReason(reason);

        stockAdjustmentRepository.saveAndFlush(
                adjustment
        );

        return toDetailResponse(adjustment);
    }

    // =========================================================
    // PENDING COUNT
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public long getPendingCount() {

        return stockAdjustmentRepository.countByStatus(
                StockAdjustmentStatus
                        .PENDING_APPROVAL
                        .getCode()
        );
    }

    // =========================================================
    // BUILD ITEMS
    // =========================================================

    private void buildItems(
            StockAdjustment adjustment,
            List<StockAdjustmentItemRequest> requests
    ) {

        Set<Integer> lotIds =
                new HashSet<>();

        for (int i = 0; i < requests.size(); i++) {

            int line = i + 1;

            StockAdjustmentItemRequest request =
                    requests.get(i);

            if (request == null) {
                throw badRequest(
                        "Dòng "
                                + line
                                + ": dữ liệu không hợp lệ."
                );
            }

            Integer lotId =
                    request.getInventoryLotId();

            if (lotId == null || lotId <= 0) {
                throw badRequest(
                        "Dòng "
                                + line
                                + ": InventoryLotId phải lớn hơn 0."
                );
            }

            if (!lotIds.add(lotId)) {
                throw badRequest(
                        "Dòng "
                                + line
                                + ": lô hàng bị trùng trong cùng phiếu."
                );
            }

            Integer actualQuantity =
                    request.getActualQuantity();

            if (actualQuantity == null
                    || actualQuantity < 0) {

                throw badRequest(
                        "Dòng "
                                + line
                                + ": số lượng thực tế phải lớn hơn hoặc bằng 0."
                );
            }

            InventoryLot lot =
                    entityManager.find(
                            InventoryLot.class,
                            lotId
                    );

            if (lot == null) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Dòng "
                                + line
                                + ": không tìm thấy lô hàng."
                );
            }

            int systemQuantity =
                    safeInt(
                            lot.getQuantityOnHand()
                    );

            int difference =
                    actualQuantity
                            - systemQuantity;

            String reason =
                    normalizeOptional(
                            request.getReason()
                    );

            if (difference != 0
                    && reason == null) {

                throw badRequest(
                        "Dòng "
                                + line
                                + ": lô "
                                + lot.getLotCode()
                                + " có chênh lệch "
                                + signed(difference)
                                + " nên bắt buộc nhập lý do."
                );
            }

            StockAdjustmentItem item =
                    new StockAdjustmentItem();

            item.setInventoryLot(lot);
            item.setSystemQuantity(systemQuantity);
            item.setActualQuantity(actualQuantity);
            item.setReason(reason);

            adjustment.addItem(item);
        }
    }

    private void postStockMovement(
            Integer inventoryLotId,
            byte movementType,
            int quantityChange,
            Integer createdBy,
            Integer referenceId,
            Integer referenceLineId,
            String reason,
            int expectedQuantityBefore
    ) {

        try {

            jdbcTemplate.execute(
                    """
                            EXEC dbo.usp_PostStockMovement
                                @InventoryLotId = ?,
                                @MovementType = ?,
                                @QuantityChange = ?,
                                @CreatedBy = ?,
                                @ReferenceType = ?,
                                @ReferenceId = ?,
                                @ReferenceLineId = ?,
                                @Reason = ?,
                                @ExpectedQuantityBefore = ?
                            """,
                    (PreparedStatementCallback<Void>) statement -> {

                        statement.setObject(
                                1,
                                inventoryLotId
                        );

                        statement.setByte(
                                2,
                                movementType
                        );

                        statement.setInt(
                                3,
                                quantityChange
                        );

                        statement.setObject(
                                4,
                                createdBy
                        );

                        statement.setString(
                                5,
                                REFERENCE_TYPE
                        );

                        statement.setObject(
                                6,
                                referenceId
                        );

                        statement.setObject(
                                7,
                                referenceLineId
                        );

                        statement.setString(
                                8,
                                reason
                        );

                        statement.setInt(
                                9,
                                expectedQuantityBefore
                        );

                        boolean hasResultSet =
                                statement.execute();

                        while (true) {

                            if (hasResultSet) {

                                try (var resultSet =
                                             statement.getResultSet()) {

                                    while (resultSet != null
                                            && resultSet.next()) {
                                        // Procedure có trả result set thì consume hết.
                                    }
                                }

                            } else {

                                int updateCount =
                                        statement.getUpdateCount();

                                if (updateCount == -1) {
                                    break;
                                }
                            }

                            hasResultSet =
                                    statement.getMoreResults();
                        }

                        return null;
                    }
            );

        } catch (DataAccessException ex) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    extractDatabaseMessage(ex),
                    ex
            );
        }
    }

    // =========================================================
    // PERMISSION
    // =========================================================

    /**
     * DRAFT:
     * - Người tạo được sửa/gửi.
     * - OWNER toàn quyền.
     */
    private void ensureDraftOwnerOrOwner(
            StockAdjustment adjustment,
            User currentUser,
            String message
    ) {

        if (isOwner(currentUser)) {
            return;
        }

        if (adjustment.getCreatedBy() != null
                && adjustment.getCreatedBy()
                .getId()
                .equals(currentUser.getId())) {

            return;
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                message
        );
    }

    /**
     * OWNER:
     * - toàn quyền
     * - được tự duyệt/từ chối phiếu mình tạo.
     * <p>
     * MANAGER:
     * - được duyệt/từ chối
     * - KHÔNG được xử lý phiếu do chính mình tạo.
     */
    private void ensureReviewerPermission(
            StockAdjustment adjustment,
            User currentUser
    ) {

        String role =
                roleName(currentUser);

        if ("OWNER".equals(role)) {
            return;
        }

        if (!"MANAGER".equals(role)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Chỉ OWNER hoặc MANAGER được xử lý phiếu kiểm kê."
            );
        }

        if (adjustment.getCreatedBy() != null
                && adjustment.getCreatedBy()
                .getId()
                .equals(currentUser.getId())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "MANAGER không được tự phê duyệt hoặc từ chối phiếu kiểm kê do chính mình tạo."
            );
        }
    }

    private boolean isOwner(User user) {
        return "OWNER".equals(
                roleName(user)
        );
    }

    private String roleName(User user) {

        if (user == null
                || user.getRole() == null
                || user.getRole().getName() == null) {

            return "";
        }

        return user.getRole()
                .getName()
                .trim()
                .toUpperCase(Locale.ROOT)
                .replace("ROLE_", "");
    }

    // =========================================================
    // CURRENT USER
    // =========================================================

    /**
     * Giả định authentication.getName() là email đăng nhập,
     * phù hợp với Users.Email của project.
     */
    private User getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || authentication.getName().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Không xác định được người dùng hiện tại."
            );
        }

        String email =
                authentication.getName().trim();

        try {

            return entityManager
                    .createQuery(
                            """
                                    select u
                                    from User u
                                    join fetch u.role
                                    where lower(u.email) = lower(:email)
                                    """,
                            User.class
                    )
                    .setParameter("email", email)
                    .setMaxResults(1)
                    .getSingleResult();

        } catch (NoResultException ex) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Không tìm thấy tài khoản đăng nhập."
            );
        }
    }

    // =========================================================
    // RESPONSE
    // =========================================================

    private StockAdjustmentListResponse toListResponse(
            StockAdjustment adjustment
    ) {

        StockAdjustmentListResponse response =
                new StockAdjustmentListResponse();

        response.setId(adjustment.getId());
        response.setAdjustmentNo(
                adjustment.getAdjustmentNo()
        );

        StockAdjustmentStatus status =
                statusOf(
                        adjustment.getStatus()
                );

        response.setStatus(status);
        response.setStatusLabel(
                status != null
                        ? status.getLabel()
                        : "—"
        );

        Summary summary =
                summarize(
                        adjustment.getItems()
                );

        applySummary(response, summary);

        response.setCreatedById(
                userId(
                        adjustment.getCreatedBy()
                )
        );

        response.setCreatedByName(
                userName(
                        adjustment.getCreatedBy()
                )
        );

        response.setCreatedAt(
                adjustment.getCreatedAt()
        );

        response.setSubmittedAt(
                adjustment.getSubmittedAt()
        );

        response.setApprovedAt(
                adjustment.getApprovedAt()
        );

        response.setRejectedAt(
                adjustment.getRejectedAt()
        );

        response.setCancelledAt(
                adjustment.getCancelledAt()
        );

        return response;
    }

    private StockAdjustmentDetailResponse toDetailResponse(
            StockAdjustment adjustment
    ) {

        StockAdjustmentDetailResponse response =
                new StockAdjustmentDetailResponse();

        response.setId(adjustment.getId());
        response.setAdjustmentNo(
                adjustment.getAdjustmentNo()
        );

        StockAdjustmentStatus status =
                statusOf(
                        adjustment.getStatus()
                );

        response.setStatus(status);
        response.setStatusLabel(
                status != null
                        ? status.getLabel()
                        : "—"
        );

        response.setRevision(
                buildRevision(adjustment)
        );

        response.setNote(
                adjustment.getNote()
        );

        Summary summary =
                summarize(
                        adjustment.getItems()
                );

        applySummary(response, summary);

        response.setCreatedById(
                userId(
                        adjustment.getCreatedBy()
                )
        );

        response.setCreatedByName(
                userName(
                        adjustment.getCreatedBy()
                )
        );

        response.setCreatedAt(
                adjustment.getCreatedAt()
        );

        response.setSubmittedById(
                userId(
                        adjustment.getSubmittedBy()
                )
        );

        response.setSubmittedByName(
                userName(
                        adjustment.getSubmittedBy()
                )
        );

        response.setSubmittedAt(
                adjustment.getSubmittedAt()
        );

        response.setApprovedById(
                userId(
                        adjustment.getApprovedBy()
                )
        );

        response.setApprovedByName(
                userName(
                        adjustment.getApprovedBy()
                )
        );

        response.setApprovedAt(
                adjustment.getApprovedAt()
        );

        response.setRejectedById(
                userId(
                        adjustment.getRejectedBy()
                )
        );

        response.setRejectedByName(
                userName(
                        adjustment.getRejectedBy()
                )
        );

        response.setRejectedAt(
                adjustment.getRejectedAt()
        );

        response.setRejectionReason(
                adjustment.getRejectionReason()
        );

        response.setCancelledById(
                userId(
                        adjustment.getCancelledBy()
                )
        );

        response.setCancelledByName(
                userName(
                        adjustment.getCancelledBy()
                )
        );

        response.setCancelledAt(
                adjustment.getCancelledAt()
        );

        response.setCancellationReason(
                adjustment.getCancellationReason()
        );

        response.setItems(
                adjustment.getItems()
                        .stream()
                        .map(this::toItemResponse)
                        .toList()
        );

        return response;
    }

    private StockAdjustmentItemResponse toItemResponse(
            StockAdjustmentItem item
    ) {

        StockAdjustmentItemResponse response =
                new StockAdjustmentItemResponse();

        InventoryLot lot =
                item.getInventoryLot();

        response.setId(
                item.getId()
        );

        response.setInventoryLotId(
                lot != null
                        ? lot.getId()
                        : null
        );

        response.setLotCode(
                lot != null
                        ? lot.getLotCode()
                        : null
        );

        response.setSystemQuantity(
                item.getSystemQuantity()
        );

        response.setActualQuantity(
                item.getActualQuantity()
        );

        int difference =
                calculateDifference(item);

        response.setQuantityDifference(
                difference
        );

        response.setCurrentQuantity(
                lot != null
                        ? lot.getQuantityOnHand()
                        : null
        );

        response.setResultLabel(
                differenceLabel(
                        difference
                )
        );

        response.setReason(
                item.getReason()
        );

        if (lot != null
                && lot.getId() != null) {

            LotDisplay display =
                    loadLotDisplay(
                            lot.getId()
                    );

            if (display != null) {

                response.setProductVariantId(
                        display.productVariantId
                );

                response.setSku(
                        display.sku
                );

                response.setProductName(
                        display.productName
                );

                response.setImageUrl(
                        display.imageUrl
                );
            }
        }

        return response;
    }

    // =========================================================
    // LOT DISPLAY
    // =========================================================

    /**
     * Dùng native SQL để không phụ thuộc getter cụ thể
     * của ProductVariant / Product.
     */
    private LotDisplay loadLotDisplay(
            Integer inventoryLotId
    ) {

        List<Map<String, Object>> rows =
                jdbcTemplate.queryForList(
                        """
                                SELECT
                                    IL.ProductVariantId,
                                    PV.Sku,
                                    P.Name AS ProductName,
                                    ImageData.ImageUrl AS ImageUrl
                                FROM dbo.InventoryLot IL
                                INNER JOIN dbo.ProductVariant PV
                                    ON PV.Id = IL.ProductVariantId
                                INNER JOIN dbo.Product P
                                    ON P.Id = PV.ProductId
                                OUTER APPLY (
                                    SELECT TOP 1
                                        PI.ImageUrl
                                    FROM dbo.ProductImage PI
                                    WHERE PI.ProductId = P.Id
                                    ORDER BY
                                        CASE
                                            WHEN PI.IsPrimary = 1 THEN 0
                                            ELSE 1
                                        END,
                                        PI.Id ASC
                                ) ImageData
                                WHERE IL.Id = ?
                                """,
                        inventoryLotId
                );

        if (rows.isEmpty()) {
            return null;
        }

        Map<String, Object> row =
                rows.get(0);

        Number productVariantId =
                (Number) row.get(
                        "ProductVariantId"
                );

        Object sku =
                row.get("Sku");

        Object productName =
                row.get("ProductName");

        Object imageUrl =
                row.get("ImageUrl");

        return new LotDisplay(
                productVariantId != null
                        ? productVariantId.intValue()
                        : null,
                sku != null
                        ? sku.toString()
                        : null,
                productName != null
                        ? productName.toString()
                        : null,
                imageUrl != null
                        ? imageUrl.toString()
                        : null
        );
    }

    private record LotDisplay(
            Integer productVariantId,
            String sku,
            String productName,
            String imageUrl
    ) {
    }

    // =========================================================
    // SUMMARY
    // =========================================================

    private Summary summarize(
            List<StockAdjustmentItem> items
    ) {

        Summary summary =
                new Summary();

        if (items == null) {
            return summary;
        }

        summary.totalLots =
                items.size();

        for (StockAdjustmentItem item : items) {

            int difference =
                    calculateDifference(item);

            if (difference == 0) {
                summary.matchedLots++;
            } else if (difference > 0) {
                summary.mismatchLots++;
                summary.increasedLots++;
                summary.totalIncrease += difference;
            } else {
                summary.mismatchLots++;
                summary.decreasedLots++;
                summary.totalDecrease += Math.abs(difference);
            }
        }

        return summary;
    }

    private void applySummary(
            StockAdjustmentListResponse response,
            Summary summary
    ) {

        response.setTotalLots(
                summary.totalLots
        );

        response.setMatchedLots(
                summary.matchedLots
        );

        response.setMismatchLots(
                summary.mismatchLots
        );

        response.setIncreasedLots(
                summary.increasedLots
        );

        response.setDecreasedLots(
                summary.decreasedLots
        );

        response.setTotalIncrease(
                summary.totalIncrease
        );

        response.setTotalDecrease(
                summary.totalDecrease
        );
    }

    private void applySummary(
            StockAdjustmentDetailResponse response,
            Summary summary
    ) {

        response.setTotalLots(
                summary.totalLots
        );

        response.setMatchedLots(
                summary.matchedLots
        );

        response.setMismatchLots(
                summary.mismatchLots
        );

        response.setIncreasedLots(
                summary.increasedLots
        );

        response.setDecreasedLots(
                summary.decreasedLots
        );

        response.setTotalIncrease(
                summary.totalIncrease
        );

        response.setTotalDecrease(
                summary.totalDecrease
        );
    }

    private static class Summary {

        private int totalLots;
        private int matchedLots;
        private int mismatchLots;
        private int increasedLots;
        private int decreasedLots;
        private int totalIncrease;
        private int totalDecrease;
    }

    // =========================================================
    // ENTITY HELPERS
    // =========================================================

    private StockAdjustment requireAdjustment(
            Integer id
    ) {

        validateId(id);

        return stockAdjustmentRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Không tìm thấy phiếu kiểm kê."
                        )
                );
    }

    private StockAdjustment requireAdjustmentForUpdate(
            Integer id
    ) {

        validateId(id);

        return stockAdjustmentRepository
                .findByIdForUpdate(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Không tìm thấy phiếu kiểm kê."
                        )
                );
    }

    private void requireStatus(
            StockAdjustment adjustment,
            StockAdjustmentStatus status,
            String message
    ) {

        if (adjustment.getStatus() == null
                || adjustment.getStatus()
                != status.getCode()) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    message
            );
        }
    }

    private StockAdjustmentStatus statusOf(
            Byte code
    ) {

        try {
            return StockAdjustmentStatus.fromCode(
                    code
            );
        } catch (IllegalArgumentException ex) {

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Trạng thái phiếu kiểm kê không hợp lệ trong dữ liệu.",
                    ex
            );
        }
    }

    // =========================================================
    // STALE / REVISION
    // =========================================================

    private void validateExpectedRevision(
            String expectedRevision,
            String currentRevision
    ) {

        String expected =
                normalizeOptional(expectedRevision);

        if (expected == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Dữ liệu phiếu kiểm kê trên màn hình chưa có phiên bản hiện tại. "
                            + "Vui lòng tải lại phiếu và xác nhận lại."
            );
        }

        if (!expected.equals(currentRevision)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Phiếu kiểm kê đã được thay đổi ở nơi khác. "
                            + "Vui lòng tải lại dữ liệu mới nhất và xác nhận lại."
            );
        }
    }

    private String buildRevision(
            StockAdjustment adjustment
    ) {

        StringBuilder source =
                new StringBuilder();

        appendRevisionPart(source, adjustment == null ? null : adjustment.getId());
        appendRevisionPart(source, adjustment == null ? null : adjustment.getAdjustmentNo());
        appendRevisionPart(source, adjustment == null ? null : adjustment.getStatus());
        appendRevisionPart(source, adjustment == null ? null : adjustment.getNote());
        appendRevisionPart(source, adjustment == null ? null : adjustment.getCreatedAt());

        if (adjustment != null
                && adjustment.getItems() != null) {

            for (StockAdjustmentItem item
                    : adjustment.getItems()) {

                InventoryLot lot =
                        item.getInventoryLot();

                appendRevisionPart(source, item.getId());
                appendRevisionPart(source, lot != null ? lot.getId() : null);
                appendRevisionPart(source, item.getSystemQuantity());
                appendRevisionPart(source, item.getActualQuantity());
                appendRevisionPart(source, item.getReason());
            }
        }

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(
                            source.toString()
                                    .getBytes(StandardCharsets.UTF_8)
                    );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException ex) {

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Không thể tạo phiên bản dữ liệu phiếu kiểm kê.",
                    ex
            );
        }
    }

    private void appendRevisionPart(
            StringBuilder source,
            Object value
    ) {

        String text =
                value == null
                        ? "<NULL>"
                        : String.valueOf(value);

        source.append(text.length())
                .append(':')
                .append(text)
                .append('|');
    }

    // =========================================================
    // VALIDATE
    // =========================================================

    private void validateRequest(
            StockAdjustmentSaveRequest request
    ) {

        if (request == null) {
            throw badRequest(
                    "Dữ liệu phiếu kiểm kê không được để trống."
            );
        }

        if (request.getItems() == null
                || request.getItems().isEmpty()) {

            throw badRequest(
                    "Phiếu kiểm kê phải có ít nhất một lô hàng."
            );
        }
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

    private void validateId(
            Integer id
    ) {

        if (id == null || id <= 0) {
            throw badRequest(
                    "Id phiếu kiểm kê không hợp lệ."
            );
        }
    }

    // =========================================================
    // NUMBER
    // =========================================================

    private String generateAdjustmentNo() {

        String date =
                LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern(
                                "yyyyMMdd"
                        )
                );

        final String chars =
                "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

        for (int attempt = 0; attempt < 20; attempt++) {

            StringBuilder suffix =
                    new StringBuilder(6);

            for (int i = 0; i < 6; i++) {
                suffix.append(
                        chars.charAt(
                                java.util.concurrent.ThreadLocalRandom
                                        .current()
                                        .nextInt(chars.length())
                        )
                );
            }

            String adjustmentNo =
                    "KK-"
                            + date
                            + "-"
                            + suffix;

            if (!stockAdjustmentRepository
                    .existsByAdjustmentNo(
                            adjustmentNo
                    )) {

                return adjustmentNo;
            }
        }

        throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Không thể tạo mã phiếu kiểm kê. Vui lòng thử lại."
        );
    }

    // =========================================================
    // SMALL HELPERS
    // =========================================================

    private int calculateDifference(
            StockAdjustmentItem item
    ) {

        return safeInt(
                item.getActualQuantity()
        ) - safeInt(
                item.getSystemQuantity()
        );
    }

    private int safeInt(Integer value) {
        return value == null
                ? 0
                : value;
    }

    private Integer userId(User user) {

        return user != null
                ? user.getId()
                : null;
    }

    private String userName(User user) {

        return user != null
                ? user.getName()
                : null;
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

    private String normalizeRequired(
            String value,
            String message
    ) {

        String normalized =
                normalizeOptional(value);

        if (normalized == null) {
            throw badRequest(message);
        }

        return normalized;
    }

    private String signed(int value) {

        return value > 0
                ? "+" + value
                : String.valueOf(value);
    }

    private String differenceLabel(
            int difference
    ) {

        if (difference > 0) {
            return "Điều chỉnh tăng "
                    + difference;
        }

        if (difference < 0) {
            return "Điều chỉnh giảm "
                    + Math.abs(difference);
        }

        return "Khớp tồn";
    }

    private String extractDatabaseMessage(
            DataAccessException ex
    ) {

        Throwable root =
                ex.getMostSpecificCause();

        if (root != null
                && root.getMessage() != null
                && !root.getMessage().isBlank()) {

            return root.getMessage();
        }

        return "Không thể cập nhật tồn kho.";
    }

    private ResponseStatusException badRequest(
            String message
    ) {

        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }
}