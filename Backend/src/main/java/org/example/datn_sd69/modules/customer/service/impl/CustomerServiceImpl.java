package org.example.datn_sd69.modules.customer.service.impl;


import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.Customer;
import org.example.datn_sd69.entity.User;
import org.example.datn_sd69.modules.customer.dto.response.CustomerResponse;
import org.example.datn_sd69.modules.customer.service.CustomerService;
import org.example.datn_sd69.repository.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerResponse> getCustomers(
            String keyword,
            Integer status,
            Pageable pageable
    ) {
        validateStatusNullable(status);

        String normalizedKeyword = normalize(keyword);

        return customerRepository
                .searchCustomers(normalizedKeyword, status, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Integer userId) {
        Customer customer = findActiveCustomer(userId);
        return mapToResponse(customer);
    }

    @Override
    @Transactional
    public CustomerResponse updateStatus(Integer userId, Integer status) {
        validateStatusRequired(status);

        Customer customer = findActiveCustomer(userId);
        User user = customer.getUser();

        user.setStatus(status);

        return mapToResponse(customer);
    }

    private Customer findActiveCustomer(Integer userId) {
        return customerRepository.findActiveByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy khách hàng"
                ));
    }

    private void validateStatusNullable(Integer status) {
        if (status != null && status != 0 && status != 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Trạng thái chỉ được là 0 hoặc 1"
            );
        }
    }

    private void validateStatusRequired(Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Trạng thái chỉ được là 0 hoặc 1"
            );
        }
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private CustomerResponse mapToResponse(Customer customer) {
        User user = customer.getUser();

        return CustomerResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .address(user.getAddress())
                .avatarUrl(user.getAvatarUrl())
                .customerRank(customer.getCustomerRank())
                .loyaltyPoints(customer.getLoyaltyPoints())
                .dateOfBirth(customer.getDateOfBirth())
                .gender(customer.getGender())
                .genderText(getGenderText(customer.getGender()))
                .status(user.getStatus())
                .statusText(getStatusText(user.getStatus()))
                .createdAt(user.getCreatedAt())
                .build();
    }

    private String getGenderText(Integer gender) {
        if (gender == null) {
            return "Chưa cập nhật";
        }

        return switch (gender) {
            case 0 -> "Nam";
            case 1 -> "Nữ";
            case 2 -> "Khác";
            default -> "Không hợp lệ";
        };
    }

    private String getStatusText(Integer status) {
        if (status == null) {
            return "Không xác định";
        }

        return switch (status) {
            case 0 -> "Đã khóa";
            case 1 -> "Đang hoạt động";
            default -> "Không hợp lệ";
        };
    }
}
