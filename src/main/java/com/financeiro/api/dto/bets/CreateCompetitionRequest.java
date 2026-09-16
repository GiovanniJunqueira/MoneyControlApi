package com.financeiro.api.dto.bets;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCompetitionRequest(
        @NotBlank(message = "Nome obrigatório.") String name,
        @NotNull(message = "Ano obrigatório.") @Min(value = 2000, message = "Ano inválido.") @Max(value = 2100, message = "Ano inválido.") Integer year,
        @NotNull(message = "Mês obrigatório.") @Min(value = 1, message = "Mês inválido.") @Max(value = 12, message = "Mês inválido.") Integer month
) {
}
