package com.financeiro.api.dto.bets;

import java.math.BigDecimal;
import java.util.List;

public record BetOverviewResponse(
        BigDecimal totalProfitLoss,
        BigDecimal totalProfitLossUnits,
        BigDecimal totalBanca,
        BigDecimal totalBancaUnits,
        BigDecimal currentUnitValue,
        List<BetGroupBancaResponse> groupBanca
) {
}
