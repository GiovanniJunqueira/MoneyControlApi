package com.financeiro.api.controller;

import com.financeiro.api.dto.expense.ExpenseListResponse;
import com.financeiro.api.dto.expense.ExpenseRequest;
import com.financeiro.api.dto.expense.ExpenseResponse;
import com.financeiro.api.dto.expense.ExpenseUpdateRequest;
import com.financeiro.api.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/tabs/{tabId}/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    // GET /tabs/{tabId}/expenses?period=2026-08  (se omitido, usa o período fiscal atual da aba)
    @GetMapping
    public ExpenseListResponse list(@PathVariable UUID tabId, @RequestParam(required = false) String period) {
        return expenseService.list(tabId, period);
    }

    @PostMapping
    public ResponseEntity<ExpenseResponse> create(@PathVariable UUID tabId, @Valid @RequestBody ExpenseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(expenseService.create(tabId, request));
    }

    @PutMapping("/{id}")
    public ExpenseResponse update(@PathVariable UUID tabId, @PathVariable UUID id, @Valid @RequestBody ExpenseUpdateRequest request) {
        return expenseService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID tabId, @PathVariable UUID id,
                                        @RequestParam(required = false, defaultValue = "false") boolean applyToFuture) {
        expenseService.delete(id, applyToFuture);
        return ResponseEntity.noContent().build();
    }
}
