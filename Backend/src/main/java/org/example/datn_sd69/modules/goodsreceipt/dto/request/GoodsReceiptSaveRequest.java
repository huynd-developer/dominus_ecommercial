package org.example.datn_sd69.modules.goodsreceipt.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.example.datn_sd69.enums.GoodsReceiptType;

import java.util.List;

@Getter
@Setter
public class GoodsReceiptSaveRequest {

    @NotNull(message = "Loại phiếu nhập không được để trống")
    private GoodsReceiptType receiptType;

    @Size(max = 1000, message = "Ghi chú phiếu không được vượt quá 1000 ký tự")
    private String note;

    @NotNull(message = "Danh sách sản phẩm không được để trống")
    @Size(min = 1, message = "Phiếu nhập phải có ít nhất một sản phẩm")
    private List<@NotNull(message = "Dòng sản phẩm không được để trống") @Valid GoodsReceiptItemRequest> items;

    /*
     * Snapshot phiên bản dữ liệu mà FE đang nhìn thấy.
     *
     * - CREATE: FE không cần gửi.
     * - UPDATE DRAFT: FE phải gửi revision nhận từ GoodsReceiptDetailResponse.
     *
     * SHA-256 hiện tại được build thành đúng 64 ký tự hex.
     * Null vẫn hợp lệ ở CREATE; UPDATE sẽ được service bắt buộc.
     */
    @Size(min = 64, max = 64, message = "Revision phiếu nhập phải có đúng 64 ký tự")
    @Pattern(
            regexp = "^[0-9a-fA-F]{64}$",
            message = "Revision phiếu nhập không đúng định dạng"
    )
    private String expectedRevision;
}
