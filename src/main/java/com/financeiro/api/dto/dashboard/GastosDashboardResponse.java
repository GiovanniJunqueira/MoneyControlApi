package com.financeiro.api.dto.dashboard;

import com.financeiro.api.dto.expense.ExpenseResponse;
import com.financeiro.api.util.FiscalPeriod;

import java.util.List;

public record GastosDashboardResponse(
        FiscalPeriod period,
        GastosResumo resumo,
        List<CategoriaResumo> porCategoria,
        List<ExpenseResponse> lancamentos
) {
}
