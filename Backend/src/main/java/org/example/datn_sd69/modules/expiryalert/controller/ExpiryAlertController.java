package org.example.datn_sd69.modules.expiryalert.controller;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.enums.ExpiryAlertGroup;
import org.example.datn_sd69.modules.expiryalert.dto.response.ExpiryAlertListResponse;
import org.example.datn_sd69.modules.expiryalert.dto.response.ExpiryAlertSummaryResponse;
import org.example.datn_sd69.modules.expiryalert.service.ExpiryAlertService;
import org.example.datn_sd69.modules.inventorylot.dto.response.InventoryLotDetailResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/expiry-alerts")
@RequiredArgsConstructor
@Validated
public class ExpiryAlertController {

    private final ExpiryAlertService expiryAlertService;


    @GetMapping("/summary")
    @PreAuthorize(
            "hasAnyAuthority('OWNER', 'MANAGER', 'CASHIER')"
    )
    public ResponseEntity<ExpiryAlertSummaryResponse> getSummary() {

        return ResponseEntity.ok(
                expiryAlertService.getSummary()
        );
    }


    @GetMapping
    @PreAuthorize(
            "hasAnyAuthority('OWNER', 'MANAGER', 'CASHIER')"
    )
    public ResponseEntity<Page<ExpiryAlertListResponse>> getList(

            @RequestParam(
                    name = "group",
                    defaultValue = "NEAR_EXPIRY"
            )
            ExpiryAlertGroup group,

            @RequestParam(
                    name = "keyword",
                    required = false
            )
            String keyword,

            @RequestParam(
                    name = "fromDays",
                    required = false
            )
            Integer fromDays,

            @RequestParam(
                    name = "toDays",
                    required = false
            )
            Integer toDays,

            @RequestParam(
                    name = "page",
                    defaultValue = "0"
            )
            int page,

            @RequestParam(
                    name = "size",
                    defaultValue = "20"
            )
            int size
    ) {

        validatePagination(page, size);

        Pageable pageable =
                PageRequest.of(page, size);

        return ResponseEntity.ok(
                expiryAlertService.getList(
                        group,
                        keyword,
                        fromDays,
                        toDays,
                        pageable
                )
        );
    }


    @GetMapping("/{id}")
    @PreAuthorize(
            "hasAnyAuthority('OWNER', 'MANAGER', 'CASHIER')"
    )
    public ResponseEntity<InventoryLotDetailResponse> getDetail(
            @PathVariable Integer id
    ) {

        return ResponseEntity.ok(
                expiryAlertService.getDetail(id)
        );
    }


    private void validatePagination(
            int page,
            int size
    ) {

        if (page < 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Trang phải lớn hơn hoặc bằng 0."
            );
        }

        if (size < 1 || size > 100) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Số bản ghi phải từ 1 đến 100."
            );
        }
    }
}