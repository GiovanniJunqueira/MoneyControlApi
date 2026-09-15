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

    // GET /bets/months/current -> 200 com o mes aberto, ou 204 se ninguem iniciou um mes ainda
    @GetMapping("/bets/months/current")
    public ResponseEntity<BetMonthResponse> currentMonth() {
        BetMonthResponse response = betService.currentMonth();
        return response == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(response);
    }

    @GetMapping("/bets/months")
    public List<BetMonthHistoryResponse> listMonths() {
        return betService.listMonthsHistory();
    }

    @PostMapping("/bets/months")
    public ResponseEntity<BetMonthResponse> startMonth(@Valid @RequestBody StartMonthRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(betService.startMonth(request));
    }

    @PutMapping("/bets/unit-value")
    public BetMonthResponse updateUnitValue(@Valid @RequestBody UpdateUnitValueRequest request) {
        return betService.updateUnitValue(request);
    }

    @PostMapping("/bets/houses/{id}/balance")
    public BetHouseBalanceResponse updateBalance(@PathVariable UUID id, @Valid @RequestBody UpdateBalanceRequest request) {
        return betService.updateHouseBalance(id, request);
    }
}
