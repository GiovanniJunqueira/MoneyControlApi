package com.financeiro.api.dto.debt;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record DebtRequest(
        @NotNull(message = "Pessoa obrigatória.") UUID debtorId,
        @NotNull(message = "Valor obrigatório.") @DecimalMin(value = "0.01", message = "O valor precisa ser maior que zero.") BigDecimal amount,
        @NotBlank(message = "Descreva o motivo da dívida.") String reason,
        @NotNull(message = "Data obrigatória.") LocalDate date
) {
}
