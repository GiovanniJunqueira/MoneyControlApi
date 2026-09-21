package com.financeiro.api.dto.investment;

import java.math.BigDecimal;
import java.util.List;

public record InvestmentProjectionResponse(
        int months,
        BigDecimal totalAmount,
        BigDecimal projectedTotal,
        BigDecimal projectedYield,
        List<InvestmentProjectionItemResponse> items
) {
}
