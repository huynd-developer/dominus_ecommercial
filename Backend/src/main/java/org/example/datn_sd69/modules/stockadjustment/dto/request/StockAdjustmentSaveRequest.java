package org.example.datn_sd69.modules.stockadjustment.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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

    @NotEmpty(
            message = "Phiếu kiểm kê phải có ít nhất một lô hàng"
    )
    private List<
            @NotNull(message = "Dòng kiểm kê không được để trống")
            @Valid
                    StockAdjustmentItemRequest
            > items;

    /*
     * Snapshot phiên bản phiếu mà FE đang nhìn thấy.
     *
     * - CREATE: null, không cần gửi.
     * - UPDATE DRAFT: SHA-256 do BE sinh, đúng 64 ký tự hex.
     *
     * Null vẫn hợp lệ cho CREATE. UPDATE tiếp tục được service bắt buộc
     * bằng validateExpectedRevision(), giữ nguyên flow stale/lost-update.
     */
    @Size(
            min = 64,
            max = 64,
            message = "Revision phiếu kiểm kê phải có đúng 64 ký tự"
    )
    @Pattern(
            regexp = "^[0-9a-fA-F]{64}$",
            message = "Revision phiếu kiểm kê không đúng định dạng"
    )
    private String expectedRevision;
}