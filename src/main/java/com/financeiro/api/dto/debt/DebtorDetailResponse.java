package com.financeiro.api.dto.debt;

import com.financeiro.api.util.FiscalPeriod;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record DebtorDetailResponse(
        UUID id, String name, String notes, BigDecimal totalDevido, FiscalPeriod period, List<DebtResponse> debts
) {
}
