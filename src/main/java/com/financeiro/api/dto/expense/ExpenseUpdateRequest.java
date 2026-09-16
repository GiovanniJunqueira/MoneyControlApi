package com.financeiro.api.dto.expense;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

// Igual ao ExpenseRequest, mas sem os campos de recurrência (não dá pra mudar o tipo de recorrência
// de um gasto já criado) e com applyToFuture, pra editar de uma vez essa ocorrência e as futuras.
public record ExpenseUpdateRequest(
        @NotNull(message = "Categoria obrigatória.") UUID categoryId,
        @NotNull(message = "Valor obrigatório.") @DecimalMin(value = "0.01", message = "O valor precisa ser maior que zero.") BigDecimal amount,
        String description,
        @NotNull(message = "Data obrigatória.") LocalDate date,
        /** true = aplica categoria/valor/descrição a essa e a todas as ocorrências futuras da mesma
         * recorrência (a data de cada uma continua a que já tinha - só essa aqui pode mudar a própria). */
        boolean applyToFuture
) {
}
