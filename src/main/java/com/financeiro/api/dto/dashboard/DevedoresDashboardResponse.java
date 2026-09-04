package com.financeiro.api.dto.dashboard;

import com.financeiro.api.util.FiscalPeriod;

import java.util.List;

public record DevedoresDashboardResponse(FiscalPeriod period, DevedoresResumo resumo, List<PessoaResumo> porPessoa) {
}
