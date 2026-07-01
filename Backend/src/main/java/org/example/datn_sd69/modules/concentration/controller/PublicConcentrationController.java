package org.example.datn_sd69.modules.concentration.controller;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.Concentration;
import org.example.datn_sd69.modules.concentration.service.ConcentrationService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/concentrations")
@RequiredArgsConstructor
public class PublicConcentrationController {
    private final ConcentrationService concentrationService;

    @GetMapping
    public ResponseEntity<Page<Concentration>> getActiveConcentrations(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "5") int size) {

        return ResponseEntity.ok(concentrationService.getActiveConcentrations(page, size));
    }
}
