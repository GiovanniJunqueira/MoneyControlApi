package com.financeiro.api.dto.bets;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record StartMonthRequest(
        @NotNull(message = "Valor da unidade obrigatório.") @DecimalMin(value = "0.01", message = "O valor da unidade precisa ser maior que zero.") BigDecimal initialUnitValue
) {
}
