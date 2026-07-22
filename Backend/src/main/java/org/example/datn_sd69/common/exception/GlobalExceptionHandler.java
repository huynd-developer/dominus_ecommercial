package org.example.datn_sd69.common.exception;

import jakarta.validation.ConstraintViolationException;
import org.example.datn_sd69.modules.orderAdmin.exception.InvalidOrderStatusException;
import org.example.datn_sd69.modules.orderAdmin.exception.OrderNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex
    ) {
        Map<String, String> errors = new LinkedHashMap<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            if (error instanceof FieldError fieldError) {
                errors.put(fieldError.getField(), fieldError.getDefaultMessage());
            } else {
                errors.put(error.getObjectName(), error.getDefaultMessage());
            }
        });

        Map<String, Object> body = buildBody(
                HttpStatus.BAD_REQUEST.value(),
                "Dữ liệu không hợp lệ"
        );

        body.put("errors", errors);

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(
            ResponseStatusException ex
    ) {
        Map<String, Object> body = buildBody(
                ex.getStatusCode().value(),
                ex.getReason() != null ? ex.getReason() : "Yêu cầu không hợp lệ"
        );

        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex
    ) {
        Map<String, Object> body = buildBody(
                HttpStatus.BAD_REQUEST.value(),
                "Dữ liệu gửi lên không đúng định dạng. Vui lòng kiểm tra role, ngày tháng hoặc kiểu dữ liệu."
        );

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex
    ) {
        Map<String, Object> body = buildBody(
                HttpStatus.BAD_REQUEST.value(),
                "Tham số không đúng định dạng: " + ex.getName()
        );

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(
            ConstraintViolationException ex
    ) {
        Map<String, Object> body = buildBody(
                HttpStatus.BAD_REQUEST.value(),
                "Dữ liệu không hợp lệ"
        );

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex
    ) {
        Map<String, Object> body = buildBody(
                HttpStatus.BAD_REQUEST.value(),
                "Dữ liệu bị trùng hoặc vi phạm ràng buộc trong database"
        );

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            AccessDeniedException ex
    ) {
        Map<String, Object> body = buildBody(
                HttpStatus.FORBIDDEN.value(),
                "Bạn không có quyền thực hiện chức năng này"
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(
            AuthenticationException ex
    ) {
        Map<String, Object> body = buildBody(
                HttpStatus.UNAUTHORIZED.value(),
                "Bạn chưa đăng nhập hoặc phiên đăng nhập không hợp lệ"
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(
            RuntimeException ex
    ) {
        Map<String, Object> body = buildBody(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage() != null ? ex.getMessage() : "Yêu cầu không hợp lệ"
        );

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(
            Exception ex
    ) {
        Map<String, Object> body = buildBody(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Lỗi hệ thống"
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private Map<String, Object> buildBody(int status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status);
        body.put("message", message);
        return body;
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<?> handleOrderNotFound(
            OrderNotFoundException ex) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ex.getMessage());

    }

    @ExceptionHandler(InvalidOrderStatusException.class)
    public ResponseEntity<?> handleInvalidStatus(
            InvalidOrderStatusException ex) {

        return ResponseEntity
                .badRequest()
                .body(ex.getMessage());

    }
}