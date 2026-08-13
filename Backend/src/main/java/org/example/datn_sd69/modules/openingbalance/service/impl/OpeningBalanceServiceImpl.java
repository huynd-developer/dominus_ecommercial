package org.example.datn_sd69.modules.openingbalance.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.enums.GoodsReceiptStatus;
import org.example.datn_sd69.enums.GoodsReceiptType;
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

        return goodsReceiptService.submit(id);
    }

    @Override
    @Transactional
    public GoodsReceiptDetailResponse approve(Integer id) {
        requireOpeningBalance(id);

        return goodsReceiptService.approve(id);
    }

    @Override
    @Transactional
    public GoodsReceiptDetailResponse reject(
            Integer id,
            GoodsReceiptRejectRequest request
    ) {
        requireOpeningBalance(id);

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