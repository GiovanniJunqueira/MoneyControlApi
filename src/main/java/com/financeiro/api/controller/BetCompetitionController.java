package com.financeiro.api.controller;

import com.financeiro.api.dto.bets.BetCompetitionRankingResponse;
import com.financeiro.api.dto.bets.BetCompetitionResponse;
import com.financeiro.api.dto.bets.CreateCompetitionRequest;
import com.financeiro.api.dto.bets.JoinCompetitionRequest;
import com.financeiro.api.service.BetCompetitionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/bets/competitions")
public class BetCompetitionController {

    private final BetCompetitionService betCompetitionService;

    public BetCompetitionController(BetCompetitionService betCompetitionService) {
        this.betCompetitionService = betCompetitionService;
    }

    @GetMapping
    public List<BetCompetitionResponse> listMine() {
        return betCompetitionService.listMine();
    }

    @PostMapping
    public ResponseEntity<BetCompetitionResponse> create(@Valid @RequestBody CreateCompetitionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(betCompetitionService.create(request));
    }

    @PostMapping("/join")
    public BetCompetitionResponse join(@Valid @RequestBody JoinCompetitionRequest request) {
        return betCompetitionService.join(request);
    }

    @GetMapping("/{id}/ranking")
    public BetCompetitionRankingResponse ranking(@PathVariable UUID id) {
        return betCompetitionService.ranking(id);
    }
}
