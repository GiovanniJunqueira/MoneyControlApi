package com.financeiro.api.dto.bets;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateBalanceRequest(
        @NotNull(message = "Saldo obrigatório.") BigDecimal balance
) {
}
