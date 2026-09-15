package com.financeiro.api.controller;

import com.financeiro.api.dto.tab.TabRequest;
import com.financeiro.api.dto.tab.TabResponse;
import com.financeiro.api.service.TabService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tabs")
public class TabController {

    private final TabService tabService;

    public TabController(TabService tabService) {
        this.tabService = tabService;
    }

    @GetMapping
    public List<TabResponse> list() {
        return tabService.list();
    }

    @PostMapping
    public ResponseEntity<TabResponse> create(@Valid @RequestBody TabRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tabService.create(request));
    }

    @PutMapping("/{id}")
    public TabResponse update(@PathVariable UUID id, @Valid @RequestBody TabRequest request) {
        return tabService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        tabService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
