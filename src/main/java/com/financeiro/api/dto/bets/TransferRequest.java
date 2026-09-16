package com.financeiro.api.dto.bets;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransferRequest(
        @NotNull(message = "Valor obrigatório.") @DecimalMin(value = "0.01", message = "O valor precisa ser maior que zero.") BigDecimal amount,
        /** "SAQUE" (sai da casa, entra no Banco) ou "DEPOSITO" (sai do Banco, entra na casa). */
        @NotBlank(message = "Tipo obrigatório.") String type,
        /** Dia sendo atualizado - null significa hoje. */
        LocalDate date
) {
}
