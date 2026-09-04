package com.financeiro.api.dto.expense;

import com.financeiro.api.util.FiscalPeriod;

import java.util.List;

public record ExpenseListResponse(FiscalPeriod period, List<ExpenseResponse> expenses) {
}
