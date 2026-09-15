package com.financeiro.api.dto.bets;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateBalanceRequest(
        @NotNull(message = "Saldo obrigatório.") BigDecimal balance,
        /** Dia sendo atualizado - null significa hoje. Só é aceito dentro do mês aberto. */
        LocalDate date
) {
}
