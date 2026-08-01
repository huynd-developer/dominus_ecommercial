package org.example.datn_sd69.modules.adminReview.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.OrderItem;
import org.example.datn_sd69.entity.Review;
import org.example.datn_sd69.entity.ReviewMedia;
import org.example.datn_sd69.entity.User;
import org.example.datn_sd69.modules.adminReview.dto.response.PublicReviewResponse;
import org.example.datn_sd69.modules.adminReview.service.PublicReviewService;
import org.example.datn_sd69.modules.adminReview.dto.request.CreateReviewRequest;
import org.example.datn_sd69.repository.OrderItemRepository;
import org.example.datn_sd69.repository.ReviewRepository;
import org.example.datn_sd69.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PublicReviewServiceImpl implements PublicReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    public Page<PublicReviewResponse> getApprovedReviewsByProduct(Integer productId, int page, int size) {
        // Sắp xếp mới nhất lên đầu theo createdAt
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // Gọi hàm Query ở ReviewRepository
        Page<Review> reviews = reviewRepository.findApprovedReviewsByProductId(productId, pageable);

        // Map từ Entity sang DTO
        return reviews.map(this::mapToResponse);
    }

    @Override
    @Transactional
    public void createReview(CreateReviewRequest request) {
        // 1. Lấy User đang đăng nhập
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(currentUsername)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin tài khoản"));

        // 2. Lấy OrderItem (Chi tiết đơn hàng khách đánh giá)
        OrderItem orderItem = orderItemRepository.findById(request.getOrderItemId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm trong đơn hàng"));

        // Kiểm tra xem khách đã đánh giá đơn này chưa
        boolean alreadyReviewed = reviewRepository.existsByUser_IdAndOrderItem_IdAndIsDeletedFalse(currentUser.getId(), orderItem.getId());
        if (alreadyReviewed) {
            throw new RuntimeException("Bạn đã đánh giá sản phẩm này rồi!");
        }

        // 3. Map dữ liệu vào Review Entity
        Review review = new Review();
        review.setUser(currentUser);
        review.setOrderItem(orderItem);
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setApprovalStatus(0); // Set = 0 (Chờ duyệt)
        review.setIsDeleted(false);
        review.setCreatedAt(LocalDateTime.now());
        review.setEditCount(0);

        // 4. Xử lý ảnh/video nếu có đính kèm
        if (request.getMediaList() != null && !request.getMediaList().isEmpty()) {
            List<ReviewMedia> mediaEntities = request.getMediaList().stream().map(m -> {
                ReviewMedia media = new ReviewMedia();
                media.setReview(review);
                media.setMediaUrl(m.getMediaUrl());
                // Nếu Entity ReviewMedia của bạn có trường mediaType, hãy mở comment dòng dưới:
                // media.setMediaType(m.getMediaType());
                return media;
            }).collect(Collectors.toList());

            review.setReviewMedias(mediaEntities);
        }

        // 5. Lưu vào DB
        reviewRepository.save(review);
    }

    /**
     * Helper: Map từ Entity -> DTO Response
     */
    private PublicReviewResponse mapToResponse(Review entity) {
        PublicReviewResponse dto = new PublicReviewResponse();

        dto.setId(entity.getId());
        dto.setRating(entity.getRating());
        dto.setComment(entity.getComment());
        dto.setCreatedAt(entity.getCreatedAt());

        // Che tên khách hàng
        String fullName = entity.getUser().getName();
        dto.setCustomerName(maskCustomerName(fullName));

        // Phiên bản sản phẩm (SKU)
        if (entity.getOrderItem() != null && entity.getOrderItem().getProductVariant() != null) {
            dto.setVariantName(entity.getOrderItem().getProductVariant().getSku());
        }

        // Map danh sách Media
        if (entity.getReviewMedias() != null && !entity.getReviewMedias().isEmpty()) {
            List<PublicReviewResponse.ReviewMediaDTO> mediaListDTO = entity.getReviewMedias().stream().map(m -> {
                PublicReviewResponse.ReviewMediaDTO mDto = new PublicReviewResponse.ReviewMediaDTO();
                mDto.setId(m.getId());
                mDto.setMediaUrl(m.getMediaUrl());
                // Tương tự, nếu DTO PublicReviewResponse có mediaType, mở comment:
                // mDto.setMediaType(m.getMediaType());
                return mDto;
            }).collect(Collectors.toList());
            dto.setMediaList(mediaListDTO);
        } else {
            dto.setMediaList(new ArrayList<>());
        }

        return dto;
    }

    /**
     * Helper: Ẩn tên khách ("Nguyễn Văn A" -> "N*** A")
     */
    private String maskCustomerName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "Khách hàng";

        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].charAt(0) + "***";
        }

        return parts[0].charAt(0) + "*** " + parts[parts.length - 1];
    }
}