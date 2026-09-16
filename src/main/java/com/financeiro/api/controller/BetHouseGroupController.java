package com.financeiro.api.controller;

import com.financeiro.api.dto.bets.BetHouseGroupRequest;
import com.financeiro.api.dto.bets.BetHouseGroupResponse;
import com.financeiro.api.service.BetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/bets/groups")
public class BetHouseGroupController {

    private final BetService betService;

    public BetHouseGroupController(BetService betService) {
        this.betService = betService;
    }

    @GetMapping
    public List<BetHouseGroupResponse> list() {
        return betService.listHouseGroups();
    }

    @PostMapping
    public ResponseEntity<BetHouseGroupResponse> create(@Valid @RequestBody BetHouseGroupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(betService.createHouseGroup(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        betService.deleteHouseGroup(id);
        return ResponseEntity.noContent().build();
    }
}
