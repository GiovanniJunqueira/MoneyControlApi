package com.financeiro.api.dto.bets;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TransferRequest(
        @NotNull(message = "Valor obrigatório.") @DecimalMin(value = "0.01", message = "O valor precisa ser maior que zero.") BigDecimal amount,
        /** "SAQUE" (sai da casa, entra na conta) ou "DEPOSITO" (sai da conta, entra na casa). */
        @NotBlank(message = "Tipo obrigatório.") String type,
        /** A outra ponta da transferência (a "conta"/banco) - a pessoa escolhe, pode ter mais de uma. */
        @NotNull(message = "Escolha a conta.") UUID counterpartHouseId,
        /** Dia sendo atualizado - null significa hoje. */
        LocalDate date
) {
}
