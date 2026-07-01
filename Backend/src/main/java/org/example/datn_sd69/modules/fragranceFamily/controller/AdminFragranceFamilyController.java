package org.example.datn_sd69.modules.fragranceFamily.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.FragranceFamily;
import org.example.datn_sd69.modules.fragranceFamily.dto.request.FragranceFamilyRequest;
import org.example.datn_sd69.modules.fragranceFamily.service.FragranceFamilyService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/fragrance-families")
@RequiredArgsConstructor
public class AdminFragranceFamilyController {
    private final FragranceFamilyService fragranceFamilyService;

    @GetMapping
    public ResponseEntity<Page<FragranceFamily>> getAllFragranceFamilies(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "5") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(fragranceFamilyService.getAll(pageable));
    }

    @PreAuthorize("hasAnyAuthority('MANAGER', 'OWNER')")
    @PostMapping
    public ResponseEntity<FragranceFamily> createFragranceFamily(@Valid @RequestBody FragranceFamilyRequest request) {
        return ResponseEntity.ok(fragranceFamilyService.create(request));
    }

    @PreAuthorize("hasAnyAuthority('MANAGER', 'OWNER')")
    @PutMapping("/{id}")
    public ResponseEntity<FragranceFamily> updateFragranceFamily(
            @PathVariable Integer id,
            @Valid @RequestBody FragranceFamilyRequest request) {
        return ResponseEntity.ok(fragranceFamilyService.update(id, request));
    }

    @PreAuthorize("hasAuthority('OWNER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteFragranceFamily(@PathVariable Integer id) {
        fragranceFamilyService.delete(id);
        return ResponseEntity.ok("Xóa nhóm hương thành công!");
    }
}
