package org.example.datn_sd69.modules.stockmovement.controller;

import lombok.RequiredArgsConstructor;

import org.example.datn_sd69.enums.StockMovementType;

import org.example.datn_sd69.modules.stockmovement.dto.response.StockMovementDetailResponse;
import org.example.datn_sd69.modules.stockmovement.dto.response.StockMovementListResponse;

import org.example.datn_sd69.modules.stockmovement.service.StockMovementService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import org.springframework.format.annotation.DateTimeFormat;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.validation.annotation.Validated;

import org.springframework.web.bind.annotation.*;

import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/stock-movements")
@RequiredArgsConstructor
@Validated
public class StockMovementController {

    private final StockMovementService stockMovementService;


    @GetMapping
    @PreAuthorize(
            "hasAnyAuthority('OWNER', 'MANAGER', 'CASHIER')"
    )
    public ResponseEntity<Page<StockMovementListResponse>> getList(

            @RequestParam(
                    name = "keyword",
                    required = false
            )
            String keyword,


            @RequestParam(
                    name = "inventoryLotId",
                    required = false
            )
            Integer inventoryLotId,


            @RequestParam(
                    name = "movementType",
                    required = false
            )
            StockMovementType movementType,


            @RequestParam(
                    name = "createdBy",
                    required = false
            )
            Integer createdBy,


            @RequestParam(
                    name = "referenceType",
                    required = false
            )
            String referenceType,


            @RequestParam(
                    name = "referenceId",
                    required = false
            )
            Integer referenceId,


            @RequestParam(
                    name = "fromDate",
                    required = false
            )
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate fromDate,


            @RequestParam(
                    name = "toDate",
                    required = false
            )
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate toDate,


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

        validatePagination(
                page,
                size
        );

        Pageable pageable =
                PageRequest.of(
                        page,
                        size
                );

        return ResponseEntity.ok(
                stockMovementService.getList(
                        keyword,

                        inventoryLotId,

                        movementType,

                        createdBy,

                        referenceType,

                        referenceId,

                        fromDate,

                        toDate,

                        pageable
                )
        );
    }


    @GetMapping("/{id}")
    @PreAuthorize(
            "hasAnyAuthority('OWNER', 'MANAGER', 'CASHIER')"
    )
    public ResponseEntity<StockMovementDetailResponse> getDetail(
            @PathVariable Integer id
    ) {

        return ResponseEntity.ok(
                stockMovementService.getDetail(id)
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