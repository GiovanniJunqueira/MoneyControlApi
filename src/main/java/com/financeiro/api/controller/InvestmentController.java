package com.financeiro.api.controller;

import com.financeiro.api.dto.investment.InvestmentProjectionResponse;
import com.financeiro.api.dto.investment.InvestmentRequest;
import com.financeiro.api.dto.investment.InvestmentResponse;
import com.financeiro.api.dto.investment.InvestmentTransactionRequest;
import com.financeiro.api.dto.investment.InvestmentsOverviewResponse;
import com.financeiro.api.service.InvestmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/investments")
public class InvestmentController {

    private final InvestmentService investmentService;

    public InvestmentController(InvestmentService investmentService) {
        this.investmentService = investmentService;
    }

    @GetMapping
    public InvestmentsOverviewResponse list() {
        return investmentService.list();
    }

    @PostMapping
    public ResponseEntity<InvestmentResponse> create(@Valid @RequestBody InvestmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(investmentService.create(request));
    }

    @PutMapping("/{id}")
    public InvestmentResponse update(@PathVariable UUID id, @Valid @RequestBody InvestmentRequest request) {
        return investmentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        investmentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/transaction")
    public InvestmentResponse transaction(@PathVariable UUID id, @Valid @RequestBody InvestmentTransactionRequest request) {
        return investmentService.applyTransaction(id, request);
    }

    @GetMapping("/projection")
    public InvestmentProjectionResponse projection(@RequestParam(defaultValue = "12") int months) {
        return investmentService.projection(months);
    }
}
