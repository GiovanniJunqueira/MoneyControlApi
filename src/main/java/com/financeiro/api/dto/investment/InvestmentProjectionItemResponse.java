package com.financeiro.api.dto.investment;

import java.math.BigDecimal;
import java.util.UUID;

public record InvestmentProjectionItemResponse(
        UUID id,
        String name,
        BigDecimal amount,
        BigDecimal projectedAmount,
        BigDecimal projectedYield
) {
}
