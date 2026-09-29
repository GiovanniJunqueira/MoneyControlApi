package com.financeiro.api.dto.expense;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ExpenseRequest(
        @NotNull(message = "Categoria obrigatória.") UUID categoryId,
        @NotNull(message = "Valor obrigatório.") @DecimalMin(value = "0.01", message = "O valor precisa ser maior que zero.") BigDecimal amount,
        String description,
        @NotNull(message = "Data obrigatória.") LocalDate date,
        /** "FIXED" (repete por recurrenceMonths meses), "INDEFINITE" (repete por um horizonte longo,
         * sem data fixa pra acabar) ou null/omitido (gasto avulso, comportamento de sempre). */
        String recurrence,
        /** Obrigatório quando recurrence = "FIXED". */
        Integer recurrenceMonths,
        /** Divide esse gasto com uma pessoa já cadastrada em Devedores - opcional. Quando presente,
         * cria automaticamente uma dívida pra essa pessoa no valor de splitAmount, ligada a esse
         * gasto. Os dois campos andam juntos: ou os dois vêm preenchidos, ou nenhum. */
        UUID splitDebtorId,
        @PositiveOrZero(message = "O valor da divisão precisa ser maior que zero.") BigDecimal splitAmount
) {
}
