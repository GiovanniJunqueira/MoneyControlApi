package com.financeiro.api.controller;

import com.financeiro.api.dto.category.CategoryRequest;
import com.financeiro.api.dto.category.CategoryResponse;
import com.financeiro.api.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tabs/{tabId}/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<CategoryResponse> list(@PathVariable UUID tabId) {
        return categoryService.list(tabId);
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@PathVariable UUID tabId, @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(tabId, request));
    }

    @PutMapping("/{id}")
    public CategoryResponse update(@PathVariable UUID tabId, @PathVariable UUID id, @Valid @RequestBody CategoryRequest request) {
        return categoryService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID tabId, @PathVariable UUID id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
