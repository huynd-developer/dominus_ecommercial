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
        System.out.println("Bắt đầu quét nước hoa hết hạn: " + LocalDate.now());

        // 1. Tìm các phân loại (variant) đã qua ngày hết hạn mà vẫn đang mở bán (status = 1)
        List<ProductVariant> expiredVariants = variantRepository.findExpiredVariants(LocalDate.now(), 1);

        if (expiredVariants.isEmpty()) {
            System.out.println("Không có phân loại nào hết hạn hôm nay.");
            return;
        }

        // Lấy danh sách ID của các Sản phẩm gốc (Product) bị ảnh hưởng để tẹo nữa tính lại giá
        Set<Integer> affectedProductIds = expiredVariants.stream()
                .map(v -> v.getProduct().getId())
                .collect(Collectors.toSet());

        // 2. Chuyển trạng thái các variant này thành 0 (Ngừng bán) và dọn giỏ hàng
        for (ProductVariant variant : expiredVariants) {
            variant.setStatus(0);
            // Xóa luôn các item trong giỏ hàng khách đang chứa cái ID variant này
            cartItemRepository.deleteByProductVariantId(variant.getId());
        }
        variantRepository.saveAll(expiredVariants);

        // 3. Kiểm tra xem sản phẩm gốc có còn biến thể nào sống sót không, nếu hết sạch thì ẩn Product
        for (Integer productId : affectedProductIds) {
            Product product = productRepository.findById(productId).orElse(null);
            if (product != null) {
                // Kiểm tra xem còn variant nào CÒN HẠN (status = 1) không
                Double minActivePrice = variantRepository.findMinSalePriceByProductIdAndStatus(productId, 1);

                if (minActivePrice == null) {
                    // Nếu trả về null nghĩa là KHÔNG CÒN biến thể nào sống sót -> Ẩn luôn sản phẩm gốc
                    product.setStatus(0);
                    productRepository.save(product);
                    System.out.println("Đã ẩn sản phẩm ID: " + productId + " do tất cả biến thể đã hết hạn.");
                }
            }
        }

        System.out.println("Hoàn tất dọn dẹp nước hoa hết hạn!");
    }
}
