package com.financeiro.api.dto.bets;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record StartMonthRequest(
        @NotNull(message = "Valor da unidade obrigatório.") @DecimalMin(value = "0.01", message = "O valor da unidade precisa ser maior que zero.") BigDecimal initialUnitValue,
        @NotNull(message = "Ano obrigatório.") @Min(value = 2000, message = "Ano inválido.") @Max(value = 2100, message = "Ano inválido.") Integer year,
        @NotNull(message = "Mês obrigatório.") @Min(value = 1, message = "Mês inválido.") @Max(value = 12, message = "Mês inválido.") Integer month
) {
}
