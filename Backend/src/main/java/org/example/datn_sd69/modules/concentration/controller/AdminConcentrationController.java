package org.example.datn_sd69.modules.concentration.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.Concentration;
import org.example.datn_sd69.modules.concentration.dto.request.ConcentrationRequest;
import org.example.datn_sd69.modules.concentration.service.ConcentrationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/concentrations")
@RequiredArgsConstructor
public class AdminConcentrationController {
    private final ConcentrationService concentrationService;

    @GetMapping
    public ResponseEntity<Page<Concentration>> getAllConcentrations(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "5") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(concentrationService.getAll(pageable));
    }

    @PreAuthorize("hasAnyAuthority('MANAGER', 'OWNER')")
    @PostMapping
    public ResponseEntity<Concentration> createConcentration(@Valid @RequestBody ConcentrationRequest request) {
        return ResponseEntity.ok(concentrationService.create(request));
    }

    @PreAuthorize("hasAnyAuthority('MANAGER', 'OWNER')")
    @PutMapping("/{id}")
    public ResponseEntity<Concentration> updateConcentration(
            @PathVariable Integer id,
            @Valid @RequestBody ConcentrationRequest request) {
        return ResponseEntity.ok(concentrationService.update(id, request));
    }

    @PreAuthorize("hasAuthority('OWNER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteConcentration(@PathVariable Integer id) {
        concentrationService.delete(id);
        return ResponseEntity.ok("Xóa nồng độ thành công!");
    }
}
