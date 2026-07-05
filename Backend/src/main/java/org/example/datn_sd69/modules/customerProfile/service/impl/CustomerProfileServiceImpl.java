package org.example.datn_sd69.modules.customerProfile.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.Customer;
import org.example.datn_sd69.entity.User;
import org.example.datn_sd69.modules.customerProfile.dto.ChangePasswordRequest;
import org.example.datn_sd69.modules.customerProfile.dto.CustomerProfileResponse;
import org.example.datn_sd69.modules.customerProfile.dto.UpdateCustomerProfileRequest;
import org.example.datn_sd69.modules.customerProfile.service.CustomerProfileService;
import org.example.datn_sd69.repository.CustomerRepository;
import org.example.datn_sd69.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Period;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CustomerProfileServiceImpl implements CustomerProfileService {

    private static final Pattern NAME_PATTERN =
            Pattern.compile("^[\\p{L}]+(?: [\\p{L}]+)*$");

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^0\\d{9}$");

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&.#])[A-Za-z\\d@$!%*?&.#]{8,50}$");

    private static final long MAX_AVATAR_SIZE = 5 * 1024 * 1024;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final Cloudinary cloudinary;

    @Override
    @Transactional(readOnly = true)
    public CustomerProfileResponse getProfile() {
        User user = getCurrentUser();
        Customer customer = getCustomerByUserId(user.getId());

        return mapToProfileResponse(user, customer);
    }

    @Override
    @Transactional
    public CustomerProfileResponse updateProfile(UpdateCustomerProfileRequest request) {
        User user = getCurrentUser();
        Customer customer = getCustomerByUserId(user.getId());

        String name = normalizeText(request.name(), "Họ tên");
        String phone = normalizeNoWhitespace(request.phone(), "Số điện thoại");
        String address = normalizeText(request.address(), "Địa chỉ");

        validateName(name);
        validatePhone(phone);
        validateAddress(address);
        validateGender(request.gender());
        validateDateOfBirth(request.dateOfBirth());

        if (userRepository.existsByPhoneAndIdNot(phone, user.getId())) {
            throw badRequest("Số điện thoại đã được sử dụng bởi tài khoản khác");
        }

        user.setName(name);
        user.setPhone(phone);
        user.setAddress(address);

        customer.setDateOfBirth(request.dateOfBirth());
        customer.setGender(request.gender());

        userRepository.save(user);
        customerRepository.save(customer);

        return mapToProfileResponse(user, customer);
    }

    @Override
    @Transactional
    public CustomerProfileResponse uploadAvatar(MultipartFile file) {
        User user = getCurrentUser();
        Customer customer = getCustomerByUserId(user.getId());

        validateAvatarFile(file);

        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "datn_sd69/customers/avatars",
                            "resource_type", "image",
                            "overwrite", true
                    )
            );

            Object secureUrl = uploadResult.get("secure_url");

            if (secureUrl == null || secureUrl.toString().trim().isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Không lấy được đường dẫn ảnh sau khi upload"
                );
            }

            user.setAvatarUrl(secureUrl.toString());
            userRepository.save(user);

            return mapToProfileResponse(user, customer);
        } catch (IOException ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Upload ảnh thất bại"
            );
        }
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = getCurrentUser();

        String oldPassword = normalizeNoWhitespace(request.oldPassword(), "Mật khẩu cũ");
        String newPassword = normalizeNoWhitespace(request.newPassword(), "Mật khẩu mới");
        String confirmPassword = normalizeNoWhitespace(request.confirmPassword(), "Xác nhận mật khẩu");

        if (!PASSWORD_PATTERN.matcher(newPassword).matches()) {
            throw badRequest("Mật khẩu mới phải có chữ hoa, chữ thường, số, ký tự đặc biệt và không chứa khoảng trắng");
        }

        if (!newPassword.equals(confirmPassword)) {
            throw badRequest("Xác nhận mật khẩu không khớp");
        }

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw badRequest("Mật khẩu cũ không chính xác");
        }

        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw badRequest("Mật khẩu mới không được trùng mật khẩu cũ");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private CustomerProfileResponse mapToProfileResponse(User user, Customer customer) {
        return new CustomerProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getAddress(),
                user.getAvatarUrl(),
                user.getStatus(),
                user.getCreatedAt(),
                customer.getCustomerRank(),
                customer.getLoyaltyPoints(),
                customer.getDateOfBirth(),
                customer.getGender()
        );
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bạn chưa đăng nhập");
        }

        String email = authentication.getName();

        if (email == null || email.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token không hợp lệ");
        }

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Không tìm thấy tài khoản đăng nhập"
                ));
    }

    private Customer getCustomerByUserId(Integer userId) {
        return customerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Tài khoản hiện tại không phải khách hàng"
                ));
    }

    private String normalizeText(String value, String fieldName) {
        if (value == null) {
            throw badRequest(fieldName + " không được để trống");
        }

        String trimmed = value.trim();

        if (trimmed.isEmpty()) {
            throw badRequest(fieldName + " không được để trống");
        }

        if (!trimmed.equals(value)) {
            throw badRequest(fieldName + " không được có khoảng trắng ở đầu hoặc cuối");
        }

        if (trimmed.contains("  ")) {
            throw badRequest(fieldName + " không được chứa nhiều khoảng trắng liên tiếp");
        }

        return trimmed;
    }

    private String normalizeNoWhitespace(String value, String fieldName) {
        if (value == null) {
            throw badRequest(fieldName + " không được để trống");
        }

        String trimmed = value.trim();

        if (trimmed.isEmpty()) {
            throw badRequest(fieldName + " không được để trống");
        }

        if (!trimmed.equals(value)) {
            throw badRequest(fieldName + " không được có khoảng trắng ở đầu hoặc cuối");
        }

        if (trimmed.chars().anyMatch(Character::isWhitespace)) {
            throw badRequest(fieldName + " không được chứa khoảng trắng");
        }

        return trimmed;
    }

    private void validateName(String name) {
        if (name.length() < 2 || name.length() > 100) {
            throw badRequest("Họ tên phải từ 2 đến 100 ký tự");
        }

        if (!NAME_PATTERN.matcher(name).matches()) {
            throw badRequest("Họ tên chỉ được chứa chữ cái và một khoảng trắng giữa các từ");
        }
    }

    private void validatePhone(String phone) {
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw badRequest("Số điện thoại phải gồm 10 số và bắt đầu bằng 0");
        }
    }

    private void validateAddress(String address) {
        if (address.length() < 5 || address.length() > 500) {
            throw badRequest("Địa chỉ phải từ 5 đến 500 ký tự");
        }
    }

    private void validateGender(Integer gender) {
        if (gender == null) {
            return;
        }

        if (gender < 0 || gender > 2) {
            throw badRequest("Giới tính không hợp lệ");
        }
    }

    private void validateDateOfBirth(LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            return;
        }

        LocalDate today = LocalDate.now();

        if (dateOfBirth.isAfter(today)) {
            throw badRequest("Ngày sinh không được lớn hơn ngày hiện tại");
        }

        int age = Period.between(dateOfBirth, today).getYears();

        if (age > 120) {
            throw badRequest("Ngày sinh không hợp lệ");
        }
    }

    private void validateAvatarFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw badRequest("Vui lòng chọn ảnh đại diện");
        }

        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw badRequest("Ảnh đại diện tối đa 5MB");
        }

        String contentType = file.getContentType();

        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw badRequest("Ảnh đại diện chỉ hỗ trợ JPG, PNG hoặc WEBP");
        }
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}