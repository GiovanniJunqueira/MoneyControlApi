package com.financeiro.api.dto.expense;

import com.financeiro.api.dto.category.CategoryResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ExpenseResponse(
        UUID id,
        BigDecimal amount,
        String description,
        LocalDate date,
        CategoryResponse category,
        UUID recurringGroupId,
        /** Preenchidos quando esse gasto é uma parcela - null nos três quando não é parcelado. */
        UUID installmentGroupId,
        Integer installmentNumber,
        Integer installmentTotal,
        /** Preenchidos quando esse gasto foi dividido com alguém - null nos três quando não foi. */
        UUID splitDebtorId,
        String splitDebtorName,
        BigDecimal splitAmount
) {
}
