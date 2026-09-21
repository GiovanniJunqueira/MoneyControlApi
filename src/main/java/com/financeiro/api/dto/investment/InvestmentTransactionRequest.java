package com.financeiro.api.dto.investment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** type: "DEPOSITO" ou "SAQUE". */
public record InvestmentTransactionRequest(@NotBlank String type, @NotNull @Positive BigDecimal amount) {
}
