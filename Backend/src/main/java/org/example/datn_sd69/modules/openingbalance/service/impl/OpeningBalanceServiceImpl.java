package org.example.datn_sd69.modules.openingbalance.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.User;
import org.example.datn_sd69.enums.GoodsReceiptStatus;
import org.example.datn_sd69.enums.GoodsReceiptType;
import org.example.datn_sd69.modules.goodsreceipt.dto.request.GoodsReceiptCancelRequest;
import org.example.datn_sd69.modules.goodsreceipt.dto.request.GoodsReceiptItemRequest;
import org.example.datn_sd69.modules.goodsreceipt.dto.request.GoodsReceiptRejectRequest;
import org.example.datn_sd69.modules.goodsreceipt.dto.request.GoodsReceiptSaveRequest;
import org.example.datn_sd69.modules.goodsreceipt.dto.response.GoodsReceiptApprovalHistoryResponse;
import org.example.datn_sd69.modules.goodsreceipt.dto.response.GoodsReceiptDetailResponse;
import org.example.datn_sd69.modules.goodsreceipt.dto.response.GoodsReceiptListResponse;
import org.example.datn_sd69.modules.goodsreceipt.service.GoodsReceiptService;
import org.example.datn_sd69.modules.openingbalance.dto.request.OpeningBalanceItemRequest;
import org.example.datn_sd69.modules.openingbalance.dto.request.OpeningBalanceSaveRequest;
import org.example.datn_sd69.modules.openingbalance.service.OpeningBalanceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OpeningBalanceServiceImpl implements OpeningBalanceService {

    private final GoodsReceiptService goodsReceiptService;

    private final EntityManager entityManager;

    private static final GoodsReceiptType TYPE =
            GoodsReceiptType.OPENING_BALANCE;

    @Override
    @Transactional(readOnly = true)
    public Page<GoodsReceiptListResponse> getList(
            String keyword,
            GoodsReceiptStatus status,
            Integer createdBy,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable
    ) {
        return goodsReceiptService.getList(
                keyword,
                status,
                TYPE,
                createdBy,
                fromDate,
                toDate,
                pageable
        );
    }

    @Override
    @Transactional(readOnly = true)
    public GoodsReceiptDetailResponse getDetail(Integer id) {
        return requireOpeningBalance(id);
    }

    @Override
    @Transactional
    public GoodsReceiptDetailResponse create(
            OpeningBalanceSaveRequest request
    ) {
        validateRequest(request);

        GoodsReceiptSaveRequest goodsReceiptRequest =
                toGoodsReceiptRequest(request);

        return goodsReceiptService.create(goodsReceiptRequest);
    }

    @Override
    @Transactional
    public GoodsReceiptDetailResponse update(
            Integer id,
            OpeningBalanceSaveRequest request
    ) {
        requireOpeningBalance(id);

        User currentUser =
                getCurrentUser();

        ensureDraftOwnerOrOwner(
                id,
                currentUser,
                "Chỉ người tạo phiếu hoặc OWNER được sửa phiếu tồn đầu kỳ."
        );

        validateRequest(request);

        return goodsReceiptService.update(
                id,
                toGoodsReceiptRequest(request)
        );
    }

    @Override
    @Transactional
    public GoodsReceiptDetailResponse submit(Integer id) {
        requireOpeningBalance(id);

        User currentUser =
                getCurrentUser();

        ensureDraftOwnerOrOwner(
                id,
                currentUser,
                "Chỉ người tạo phiếu hoặc OWNER được gửi duyệt phiếu tồn đầu kỳ."
        );

        return goodsReceiptService.submit(id);
    }

    @Override
    @Transactional
    public GoodsReceiptDetailResponse cancel(
            Integer id,
            GoodsReceiptCancelRequest request
    ) {
        requireOpeningBalance(id);

        User currentUser =
                getCurrentUser();

        ensureDraftOwnerOrOwner(
                id,
                currentUser,
                "Chỉ người tạo phiếu hoặc OWNER được hủy phiếu tồn đầu kỳ."
        );

        return goodsReceiptService.cancel(
                id,
                request
        );
    }

    @Override
    @Transactional
    public GoodsReceiptDetailResponse approve(Integer id) {
        requireOpeningBalance(id);

        User currentUser =
                getCurrentUser();

        ensureReviewerPermission(
                id,
                currentUser
        );

        return goodsReceiptService.approve(id);
    }

    @Override
    @Transactional
    public GoodsReceiptDetailResponse reject(
            Integer id,
            GoodsReceiptRejectRequest request
    ) {
        requireOpeningBalance(id);

        User currentUser =
                getCurrentUser();

        ensureReviewerPermission(
                id,
                currentUser
        );

        return goodsReceiptService.reject(id, request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GoodsReceiptApprovalHistoryResponse> getApprovalHistory(
            Integer id
    ) {
        requireOpeningBalance(id);

        return goodsReceiptService.getApprovalHistory(id);
    }

    /**
     * Không cho dùng ID của phiếu nhập thường qua API kiểm kho.
     */
    private GoodsReceiptDetailResponse requireOpeningBalance(Integer id) {

        if (id == null || id <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Id phiếu kiểm kho không hợp lệ."
            );
        }

        GoodsReceiptDetailResponse detail =
                goodsReceiptService.getDetail(id);

        if (detail.getReceiptType() != TYPE) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy phiếu kiểm kho ban đầu."
            );
        }

        return detail;
    }

    // =========================================================
    // PERMISSION
    // =========================================================

    /**
     * DRAFT:
     * - CASHIER / MANAGER chỉ được sửa hoặc gửi phiếu do mình tạo.
     * - OWNER toàn quyền.
     */
    private void ensureDraftOwnerOrOwner(
            Integer receiptId,
            User currentUser,
            String message
    ) {

        if (isOwner(currentUser)) {
            return;
        }

        String role =
                roleName(currentUser);

        if (!"MANAGER".equals(role)
                && !"CASHIER".equals(role)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    message
            );
        }

        Integer createdById =
                getReceiptCreatedById(receiptId);

        if (createdById.equals(currentUser.getId())) {
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
     * - được tự duyệt / từ chối phiếu mình tạo.
     *
     * MANAGER:
     * - được duyệt / từ chối phiếu người khác
     * - KHÔNG được tự duyệt / từ chối phiếu mình tạo.
     */
    private void ensureReviewerPermission(
            Integer receiptId,
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
                    "Chỉ OWNER hoặc MANAGER được xử lý phiếu tồn đầu kỳ."
            );
        }

        Integer createdById =
                getReceiptCreatedById(receiptId);

        if (createdById.equals(currentUser.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "MANAGER không được tự phê duyệt hoặc từ chối phiếu tồn đầu kỳ do chính mình tạo."
            );
        }
    }

    private Integer getReceiptCreatedById(
            Integer receiptId
    ) {

        Object result;

        try {
            result = entityManager
                    .createNativeQuery(
                            """
                            SELECT CreatedBy
                            FROM dbo.GoodsReceipt
                            WHERE Id = :id
                            """
                    )
                    .setParameter("id", receiptId)
                    .getSingleResult();

        } catch (NoResultException ex) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy phiếu kiểm kho ban đầu."
            );
        }

        if (!(result instanceof Number number)) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Không xác định được người tạo phiếu tồn đầu kỳ."
            );
        }

        return number.intValue();
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

    /**
     * Giữ cùng cách xác định user như module StockAdjustment:
     * authentication.getName() là email đăng nhập.
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

    /**
     * Validation nghiệp vụ.
     *
     * Dù DB đã có constraint, vẫn phải validate tại service
     * để trả lỗi API dễ hiểu.
     */
    private void validateRequest(OpeningBalanceSaveRequest request) {

        if (request == null) {
            throw badRequest("Dữ liệu phiếu kiểm kho không được để trống.");
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw badRequest(
                    "Phiếu kiểm kho phải có ít nhất một sản phẩm."
            );
        }

        Set<String> duplicateSet = new HashSet<>();

        for (int i = 0; i < request.getItems().size(); i++) {

            OpeningBalanceItemRequest item = request.getItems().get(i);
            int line = i + 1;

            if (item == null) {
                throw badRequest(
                        "Dòng " + line + ": dữ liệu sản phẩm không hợp lệ."
                );
            }

            if (item.getProductVariantId() == null
                    || item.getProductVariantId() <= 0) {
                throw badRequest(
                        "Dòng " + line
                                + ": ProductVariantId phải lớn hơn 0."
                );
            }

            if (item.getQuantity() == null
                    || item.getQuantity() <= 0) {
                throw badRequest(
                        "Dòng " + line
                                + ": số lượng thực tế phải lớn hơn 0."
                );
            }

            String lotCode = normalizeRequired(
                    item.getLotCode(),
                    "Dòng " + line + ": mã lô không được để trống."
            );

            if (lotCode.length() > 100) {
                throw badRequest(
                        "Dòng " + line
                                + ": mã lô không được vượt quá 100 ký tự."
                );
            }

            if (item.getReceivedDate() == null) {
                throw badRequest(
                        "Dòng " + line
                                + ": ngày nhận không được để trống."
                );
            }

            if (item.getExpirationDate() == null) {
                throw badRequest(
                        "Dòng " + line
                                + ": hạn sử dụng không được để trống."
                );
            }

            if (item.getExpirationDate()
                    .isBefore(item.getReceivedDate())) {

                throw badRequest(
                        "Dòng " + line
                                + ": hạn sử dụng phải lớn hơn "
                                + "hoặc bằng ngày nhận."
                );
            }

            if (item.getManufacturedDate() != null) {

                if (item.getManufacturedDate()
                        .isAfter(item.getReceivedDate())) {

                    throw badRequest(
                            "Dòng " + line
                                    + ": ngày sản xuất phải nhỏ hơn "
                                    + "hoặc bằng ngày nhận."
                    );
                }

                if (item.getManufacturedDate()
                        .isAfter(item.getExpirationDate())) {

                    throw badRequest(
                            "Dòng " + line
                                    + ": ngày sản xuất phải nhỏ hơn "
                                    + "hoặc bằng hạn sử dụng."
                    );
                }
            }

            String duplicateKey =
                    item.getProductVariantId()
                            + "|"
                            + lotCode.toUpperCase(Locale.ROOT);

            if (!duplicateSet.add(duplicateKey)) {
                throw badRequest(
                        "Dòng " + line
                                + ": không được trùng SKU + LotCode "
                                + "trong cùng phiếu."
                );
            }
        }
    }

    private GoodsReceiptSaveRequest toGoodsReceiptRequest(
            OpeningBalanceSaveRequest request
    ) {

        GoodsReceiptSaveRequest result =
                new GoodsReceiptSaveRequest();

        /*
         * QUAN TRỌNG:
         * receiptType do SERVER quyết định.
         */
        result.setReceiptType(TYPE);

        result.setNote(normalizeOptional(request.getNote()));

        List<GoodsReceiptItemRequest> items =
                request.getItems()
                        .stream()
                        .map(this::toGoodsReceiptItem)
                        .toList();

        result.setItems(items);

        return result;
    }

    private GoodsReceiptItemRequest toGoodsReceiptItem(
            OpeningBalanceItemRequest source
    ) {

        GoodsReceiptItemRequest target =
                new GoodsReceiptItemRequest();

        target.setProductVariantId(source.getProductVariantId());
        target.setLotCode(source.getLotCode().trim());
        target.setQuantity(source.getQuantity());

        /*
         * Kiểm kho ban đầu không nhập giá vốn.
         */
        target.setUnitCost(null);

        target.setManufacturedDate(source.getManufacturedDate());
        target.setReceivedDate(source.getReceivedDate());
        target.setExpirationDate(source.getExpirationDate());
        target.setNote(normalizeOptional(source.getNote()));

        return target;
    }

    private String normalizeRequired(
            String value,
            String message
    ) {
        if (value == null || value.trim().isEmpty()) {
            throw badRequest(message);
        }

        return value.trim();
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

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }
}