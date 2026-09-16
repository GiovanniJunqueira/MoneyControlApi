package com.financeiro.api.controller;

import com.financeiro.api.dto.bets.*;
import com.financeiro.api.service.BetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class BetController {

    private final BetService betService;

    public BetController(BetService betService) {
        this.betService = betService;
    }

    @GetMapping("/bets/overview")
    public BetOverviewResponse overview() {
        return betService.overview();
    }

    @GetMapping("/bets/months")
    public List<BetMonthSummaryResponse> listMonths() {
        return betService.listMonthsHistory();
    }

    @PostMapping("/bets/months")
    public ResponseEntity<BetMonthSummaryResponse> startMonth(@Valid @RequestBody StartMonthRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(betService.startMonth(request));
    }

    @GetMapping("/bets/months/{id}/days")
    public BetMonthDaysResponse listMonthDays(@PathVariable UUID id) {
        return betService.listMonthDays(id);
    }

    @DeleteMapping("/bets/months/{id}")
    public ResponseEntity<Void> deleteMonth(@PathVariable UUID id) {
        betService.deleteMonth(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/bets/unit-value")
    public ResponseEntity<Void> updateUnitValue(@Valid @RequestBody UpdateUnitValueRequest request) {
        betService.updateUnitValue(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bets/months/{monthId}/houses/{houseId}/balance")
    public ResponseEntity<Void> updateBalance(@PathVariable UUID monthId, @PathVariable UUID houseId,
                                               @Valid @RequestBody UpdateBalanceRequest request) {
        betService.updateHouseBalance(monthId, houseId, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bets/months/{monthId}/houses/{houseId}/transfer")
    public ResponseEntity<Void> transfer(@PathVariable UUID monthId, @PathVariable UUID houseId,
                                          @Valid @RequestBody TransferRequest request) {
        betService.transferWithBank(monthId, houseId, request);
        return ResponseEntity.noContent().build();
    }
}
