package com.financeiro.api.controller;

import com.financeiro.api.dto.debt.DebtorDetailResponse;
import com.financeiro.api.dto.debtor.DebtorRequest;
import com.financeiro.api.dto.debtor.DebtorSummaryResponse;
import com.financeiro.api.service.DebtorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tabs/{tabId}/debtors")
public class DebtorController {

    private final DebtorService debtorService;

    public DebtorController(DebtorService debtorService) {
        this.debtorService = debtorService;
    }

    @GetMapping
    public List<DebtorSummaryResponse> list(@PathVariable UUID tabId) {
        return debtorService.list(tabId);
    }

    @GetMapping("/{id}")
    public DebtorDetailResponse detail(@PathVariable UUID tabId, @PathVariable UUID id) {
        return debtorService.detail(id);
    }

    @PostMapping
    public ResponseEntity<DebtorSummaryResponse> create(@PathVariable UUID tabId, @Valid @RequestBody DebtorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(debtorService.create(tabId, request));
    }

    @PutMapping("/{id}")
    public DebtorSummaryResponse update(@PathVariable UUID tabId, @PathVariable UUID id, @Valid @RequestBody DebtorRequest request) {
        return debtorService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID tabId, @PathVariable UUID id) {
        debtorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
