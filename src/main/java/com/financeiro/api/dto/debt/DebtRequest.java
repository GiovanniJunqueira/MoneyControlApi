package com.financeiro.api.dto.debt;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record DebtRequest(
        @NotNull(message = "Pessoa obrigatória.") UUID debtorId,
        /** Quando parcelado (installments != null e > 1), esse é o valor TOTAL, dividido entre as parcelas. */
        @NotNull(message = "Valor obrigatório.") @DecimalMin(value = "0.01", message = "O valor precisa ser maior que zero.") BigDecimal amount,
        @NotBlank(message = "Descreva o motivo da dívida.") String reason,
        /** Data da 1ª parcela (ou a data única, se não for parcelado). */
        @NotNull(message = "Data obrigatória.") LocalDate date,
        /** null/1 = dívida avulsa. > 1 = divide o valor em N parcelas mensais a partir de `date`. */
        Integer installments
) {
}
