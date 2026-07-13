package org.example.datn_sd69.modules.promotion.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class PromotionRequest {

    @NotBlank(message = "Tên chiến dịch khuyến mãi không được để trống")
    @Size(min = 3, max = 255, message = "Tên chiến dịch khuyến mãi phải từ 3 đến 255 ký tự")
    private String name;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    @NotEmpty(message = "Phải chọn ít nhất 1 biến thể sản phẩm")
    @Size(max = 100, message = "Một chiến dịch chỉ nên áp dụng tối đa 100 biến thể")
    @Valid
    private List<PromotionVariantRequest> variants;

    @JsonIgnore
    @AssertTrue(message = "Ngày bắt đầu không được để trống")
    public boolean isStartDateValid() {
        return startDate != null;
    }

    @JsonIgnore
    @AssertTrue(message = "Ngày kết thúc không được để trống")
    public boolean isEndDateValid() {
        return endDate != null;
    }

    @JsonIgnore
    @AssertTrue(message = "Ngày kết thúc phải lớn hơn ngày bắt đầu")
    public boolean isEndDateAfterStartDate() {
        if (startDate == null || endDate == null) {
            return true;
        }

        return endDate.isAfter(startDate);
    }

    @JsonIgnore
    @AssertTrue(message = "Ngày kết thúc phải lớn hơn thời gian hiện tại")
    public boolean isEndDateAfterNow() {
        if (endDate == null) {
            return true;
        }

        return endDate.isAfter(LocalDateTime.now());
    }
}