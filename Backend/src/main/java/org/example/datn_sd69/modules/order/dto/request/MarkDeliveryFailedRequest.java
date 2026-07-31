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
public class MarkDeliveryFailedRequest {

    private String reason;

    private String description;

    /**
     * Ảnh minh chứng giao hàng thất bại.
     * Bắt buộc hay không phụ thuộc lý do, validate trong service.
     */
    private List<MultipartFile> files = new ArrayList<>();
}