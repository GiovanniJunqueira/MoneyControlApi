package com.financeiro.api.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CategoryRequest(
        @NotBlank(message = "Nome obrigatório.") String name,
        String color,
        String icon,
        /** Limite de gasto mensal opcional - null = sem orçamento. */
        @PositiveOrZero(message = "Orçamento precisa ser maior ou igual a zero.") BigDecimal monthlyBudget
) {
}
