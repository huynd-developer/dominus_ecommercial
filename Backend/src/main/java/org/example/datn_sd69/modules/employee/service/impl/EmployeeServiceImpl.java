package org.example.datn_sd69.modules.employee.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.Employee;
import org.example.datn_sd69.entity.Role;
import org.example.datn_sd69.entity.User;
import org.example.datn_sd69.enums.RoleType;
import org.example.datn_sd69.modules.employee.dto.request.EmployeeCreateRequest;
import org.example.datn_sd69.modules.employee.dto.request.EmployeeUpdateRequest;
import org.example.datn_sd69.modules.employee.dto.response.EmployeeAvatarUploadResponse;
import org.example.datn_sd69.modules.employee.dto.response.EmployeeResponse;
import org.example.datn_sd69.modules.employee.service.EmployeeService;
import org.example.datn_sd69.repository.EmployeeRepository;
import org.example.datn_sd69.repository.RoleRepository;
import org.example.datn_sd69.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private static final Set<RoleType> ALLOWED_EMPLOYEE_ROLES =
            EnumSet.of(RoleType.MANAGER, RoleType.CASHIER);

    private static final long MAX_AVATAR_SIZE = 5 * 1024 * 1024;

    private static final Set<String> ALLOWED_AVATAR_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final Cloudinary cloudinary;

    @Override
    public Page<EmployeeResponse> getAll(String keyword, Integer status, Pageable pageable) {
        validateStatusFilter(status);

        String normalizedKeyword = normalize(keyword);

        return employeeRepository.searchEmployees(normalizedKeyword, status, pageable)
                .map(this::toResponse);
    }

    @Override
    public EmployeeResponse getById(Integer userId) {
        Employee employee = getActiveEmployee(userId);
        return toResponse(employee);
    }

    @Override
    @Transactional
    public EmployeeResponse create(EmployeeCreateRequest request) {
        validateStaffRole(request.getRole());

        String email = normalizeEmail(request.getEmail());
        String phone = normalize(request.getPhone());
        String citizenId = normalize(request.getCitizenId());

        checkDuplicateUser(email, phone, null);
        checkDuplicateEmployee(citizenId, null);

        Role role = getRole(request.getRole());

        User user = new User();
        user.setRole(role);
        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword().trim()));
        user.setPhone(phone);
        user.setAddress(normalize(request.getAddress()));
        user.setAvatarUrl(normalize(request.getAvatarUrl()));
        user.setStatus(1);
        user.setIsDeleted(false);

        User savedUser = userRepository.save(user);

        Employee employee = new Employee();
        employee.setUser(savedUser);
        employee.setEmployeeCode(generateEmployeeCode(savedUser.getId()));
        employee.setCitizenId(citizenId);
        employee.setJobTitle(normalize(request.getJobTitle()));
        employee.setSalary(request.getSalary());
        employee.setHireDate(request.getHireDate());

        Employee savedEmployee = employeeRepository.save(employee);

        return toResponse(savedEmployee);
    }

    @Override
    @Transactional
    public EmployeeResponse update(Integer userId, EmployeeUpdateRequest request) {
        Employee employee = getActiveEmployee(userId);
        User user = employee.getUser();

        validateStaffRole(request.getRole());

        String email = normalizeEmail(request.getEmail());
        String phone = normalize(request.getPhone());
        String citizenId = normalize(request.getCitizenId());

        checkDuplicateUser(email, phone, userId);
        checkDuplicateEmployee(citizenId, userId);

        Role role = getRole(request.getRole());

        user.setRole(role);
        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setPhone(phone);
        user.setAddress(normalize(request.getAddress()));
        user.setAvatarUrl(normalize(request.getAvatarUrl()));

        if (StringUtils.hasText(request.getPassword())) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword().trim()));
        }

        employee.setCitizenId(citizenId);
        employee.setJobTitle(normalize(request.getJobTitle()));
        employee.setSalary(request.getSalary());
        employee.setHireDate(request.getHireDate());

        Employee savedEmployee = employeeRepository.save(employee);

        return toResponse(savedEmployee);
    }

    @Override
    @Transactional
    public EmployeeResponse lock(Integer userId) {
        Employee employee = getActiveEmployee(userId);
        User user = employee.getUser();

        preventSelfAction(user, "Không thể tự khóa chính tài khoản đang đăng nhập");

        user.setStatus(0);

        return toResponse(employee);
    }

    @Override
    @Transactional
    public EmployeeResponse unlock(Integer userId) {
        Employee employee = getActiveEmployee(userId);
        User user = employee.getUser();

        user.setStatus(1);

        return toResponse(employee);
    }

    @Override
    @Transactional
    public void deleteSoft(Integer userId) {
        Employee employee = getActiveEmployee(userId);
        User user = employee.getUser();

        preventSelfAction(user, "Không thể tự xóa chính tài khoản đang đăng nhập");

        user.setStatus(0);
        user.setIsDeleted(true);
    }

    @Override
    public EmployeeAvatarUploadResponse uploadAvatar(MultipartFile file) {
        validateAvatarFile(file);

        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "datn_sd69/employees/avatar",
                            "resource_type", "image",
                            "overwrite", true,
                            "format", "webp"
                    )
            );

            Object secureUrlObject = uploadResult.get("secure_url");
            Object publicIdObject = uploadResult.get("public_id");

            String url = secureUrlObject == null ? "" : secureUrlObject.toString();
            String publicId = publicIdObject == null ? "" : publicIdObject.toString();

            if (!StringUtils.hasText(url)) {
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Cloudinary không trả về URL ảnh"
                );
            }

            return new EmployeeAvatarUploadResponse(url, publicId);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Upload ảnh thất bại: " + e.getMessage()
            );
        }
    }

    private void validateAvatarFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Vui lòng chọn ảnh nhân viên"
            );
        }

        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ảnh nhân viên không được vượt quá 5MB"
            );
        }

        String contentType = file.getContentType();

        if (contentType == null || !ALLOWED_AVATAR_TYPES.contains(contentType)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chỉ hỗ trợ ảnh JPG, PNG hoặc WEBP. File hiện tại: " + contentType
            );
        }
    }

    private Employee getActiveEmployee(Integer userId) {
        return employeeRepository.findActiveByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy nhân viên"
                ));
    }

    private void validateStaffRole(RoleType roleType) {
        if (roleType == null || !ALLOWED_EMPLOYEE_ROLES.contains(roleType)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Nhân viên chỉ được gán quyền MANAGER hoặc CASHIER"
            );
        }
    }

    private void validateStatusFilter(Integer status) {
        if (status != null && status != 0 && status != 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Trạng thái tài khoản chỉ được là 0 hoặc 1"
            );
        }
    }

    private Role getRole(RoleType roleType) {
        return roleRepository.findByNameIgnoreCase(roleType.name())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Vai trò không tồn tại: " + roleType.name()
                ));
    }

    private void checkDuplicateUser(String email, String phone, Integer ignoreUserId) {
        if (userRepository.existsEmail(email, ignoreUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Email đã tồn tại"
            );
        }

        if (StringUtils.hasText(phone) && userRepository.existsPhone(phone, ignoreUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Số điện thoại đã tồn tại"
            );
        }
    }

    private void checkDuplicateEmployee(String citizenId, Integer ignoreUserId) {
        if (employeeRepository.existsCitizenId(citizenId, ignoreUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "CCCD đã tồn tại"
            );
        }
    }

    private void preventSelfAction(User targetUser, String message) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            return;
        }

        String currentEmail = authentication.getName().trim().toLowerCase();
        String targetEmail = targetUser.getEmail().trim().toLowerCase();

        if (currentEmail.equals(targetEmail)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private String generateEmployeeCode(Integer userId) {
        return "NV" + String.format("%05d", userId);
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private EmployeeResponse toResponse(Employee employee) {
        User user = employee.getUser();

        return EmployeeResponse.builder()
                .userId(user.getId())
                .employeeCode(employee.getEmployeeCode())
                .roleName(user.getRole().getName())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .address(user.getAddress())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus())
                .isDeleted(user.getIsDeleted())
                .createdAt(user.getCreatedAt())
                .citizenId(employee.getCitizenId())
                .jobTitle(employee.getJobTitle())
                .salary(employee.getSalary())
                .hireDate(employee.getHireDate())
                .build();
    }
}