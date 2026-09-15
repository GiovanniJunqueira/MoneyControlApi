package com.financeiro.api.controller;

import com.financeiro.api.dto.debt.DebtRequest;
import com.financeiro.api.dto.debt.DebtResponse;
import com.financeiro.api.dto.debt.DebtUpdateRequest;
import com.financeiro.api.dto.debt.PaymentRequest;
import com.financeiro.api.service.DebtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/tabs/{tabId}/debts")
public class DebtController {

    private final DebtService debtService;

    public DebtController(DebtService debtService) {
        this.debtService = debtService;
    }

    @PostMapping
    public ResponseEntity<DebtResponse> create(@PathVariable UUID tabId, @Valid @RequestBody DebtRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(debtService.create(tabId, request));
    }

    @PutMapping("/{id}")
    public DebtResponse update(@PathVariable UUID tabId, @PathVariable UUID id, @Valid @RequestBody DebtUpdateRequest request) {
        return debtService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID tabId, @PathVariable UUID id) {
        debtService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/payments")
    public ResponseEntity<DebtResponse> registerPayment(@PathVariable UUID tabId, @PathVariable UUID id, @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(debtService.registerPayment(id, request));
    }
}
