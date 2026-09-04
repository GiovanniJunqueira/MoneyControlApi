package com.financeiro.api.dto.expense;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ExpenseRequest(
        @NotNull(message = "Categoria obrigatória.") UUID categoryId,
        @NotNull(message = "Valor obrigatório.") @DecimalMin(value = "0.01", message = "O valor precisa ser maior que zero.") BigDecimal amount,
        String description,
        @NotNull(message = "Data obrigatória.") LocalDate date
) {
}
