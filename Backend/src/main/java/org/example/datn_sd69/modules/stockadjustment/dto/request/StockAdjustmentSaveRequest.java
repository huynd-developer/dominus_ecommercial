package org.example.datn_sd69.modules.stockadjustment.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class StockAdjustmentSaveRequest {

    @Size(
            max = 1000,
            message = "Ghi chú không được vượt quá 1000 ký tự"
    )
    private String note;

    @Valid
    @NotEmpty(
            message = "Phiếu kiểm kê phải có ít nhất một lô hàng"
    )
    private List<StockAdjustmentItemRequest> items;

    /*
     * Snapshot phiên bản phiếu mà FE đang nhìn thấy.
     *
     * - CREATE: không cần gửi.
     * - UPDATE DRAFT: FE gửi revision từ StockAdjustmentDetailResponse.
     *
     * Chỉ dùng để chống stale/lost-update, không phải dữ liệu nghiệp vụ.
     */
    @Size(
            max = 64,
            message = "Revision phiếu kiểm kê không hợp lệ"
    )
    private String expectedRevision;
}
