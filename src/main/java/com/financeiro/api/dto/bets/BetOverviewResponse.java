package com.financeiro.api.dto.bets;

import java.math.BigDecimal;

public record BetOverviewResponse(
        BigDecimal totalProfitLoss,
        BigDecimal totalProfitLossUnits,
        BigDecimal totalBanca,
        BigDecimal totalBancaUnits,
        BigDecimal currentUnitValue
) {
}
