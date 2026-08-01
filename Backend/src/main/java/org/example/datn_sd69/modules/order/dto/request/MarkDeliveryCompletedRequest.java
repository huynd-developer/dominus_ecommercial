package org.example.datn_sd69.modules.order.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarkDeliveryCompletedRequest {

    /**
     * Ảnh minh chứng giao hàng thành công.
     * Validate số lượng, dung lượng và định dạng xử lý trong service.
     */
    private List<MultipartFile> files = new ArrayList<>();
}