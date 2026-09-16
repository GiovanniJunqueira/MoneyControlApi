package com.financeiro.api.dto.expense;

import com.financeiro.api.dto.category.CategoryResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ExpenseResponse(
        UUID id,
        BigDecimal amount,
        String description,
        LocalDate date,
        CategoryResponse category,
        UUID recurringGroupId
) {
}
