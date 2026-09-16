package com.financeiro.api.controller;

import com.financeiro.api.dto.bets.AssignHouseGroupRequest;
import com.financeiro.api.dto.bets.BetHouseRequest;
import com.financeiro.api.dto.bets.BetHouseResponse;
import com.financeiro.api.dto.bets.ReorderHousesRequest;
import com.financeiro.api.service.BetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/bets/houses")
public class BetHouseController {

    private final BetService betService;

    public BetHouseController(BetService betService) {
        this.betService = betService;
    }

    @GetMapping
    public List<BetHouseResponse> list() {
        return betService.listHouses();
    }

    @PostMapping
    public ResponseEntity<BetHouseResponse> create(@Valid @RequestBody BetHouseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(betService.createHouse(request));
    }

    @PutMapping("/{id}")
    public BetHouseResponse update(@PathVariable UUID id, @Valid @RequestBody BetHouseRequest request) {
        return betService.updateHouse(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        betService.deleteHouse(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/reorder")
    public ResponseEntity<Void> reorder(@Valid @RequestBody ReorderHousesRequest request) {
        betService.reorderHouses(request);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/group")
    public BetHouseResponse assignGroup(@PathVariable UUID id, @Valid @RequestBody AssignHouseGroupRequest request) {
        return betService.assignHouseGroup(id, request);
    }
}
