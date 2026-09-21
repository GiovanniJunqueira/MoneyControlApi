package com.financeiro.api.dto.investment;

import java.math.BigDecimal;
import java.util.List;

public record InvestmentsOverviewResponse(BigDecimal totalInvested, List<InvestmentResponse> investments) {
}
