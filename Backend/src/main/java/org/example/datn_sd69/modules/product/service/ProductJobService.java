package org.example.datn_sd69.modules.product.service;

import org.example.datn_sd69.entity.Product;
import org.example.datn_sd69.entity.ProductVariant;
import org.example.datn_sd69.repository.CartItemRepository;
import org.example.datn_sd69.repository.ProductRepository;
import org.example.datn_sd69.repository.ProductVariantRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProductJobService {

    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;

    public ProductJobService(ProductVariantRepository variantRepository,
                             ProductRepository productRepository,
                             CartItemRepository cartItemRepository) {
        this.variantRepository = variantRepository;
        this.productRepository = productRepository;
        this.cartItemRepository = cartItemRepository;
    }

    // Cron setup: Chạy vào đúng 00:00:00 mỗi ngày
    @Scheduled(cron = "0 0 0 * * ?")
    // THÊM DÒNG NÀY: Tự động chạy 1 lần duy nhất ngay khi Server vừa Start xong!
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void processExpiredProductsJob() {
        /*
         * InventoryLot là nguồn NSX/HSD và tồn kho vật lý duy nhất.
         *
         * ProductVariant.expirationDate là legacy/compatibility nên job này
         * tuyệt đối không được:
         * - tìm variant hết hạn bằng ProductVariant.expirationDate;
         * - tự set ProductVariant.status = 0;
         * - xóa CartItem;
         * - tự ẩn Product chỉ vì ngày HSD legacy.
         *
         * SKU còn bán được hay không được xác định động từ InventoryLot:
         * QuantityOnHand > 0 AND ExpirationDate >= ngày hiện tại.
         *
         * Giữ nguyên method + lịch chạy để không ảnh hưởng wiring/caller cũ,
         * nhưng không còn thực hiện mutation nghiệp vụ legacy.
         */
        System.out.println(
                "Bỏ qua ProductVariant expiration legacy job; sellability được xác định từ InventoryLot."
        );
    }
}