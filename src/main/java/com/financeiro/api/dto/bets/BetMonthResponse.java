package com.financeiro.api.dto.bets;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record BetMonthResponse(
        UUID id,
        LocalDate startDate,
        BigDecimal unitValue,
        BigDecimal totalBanca,
        BigDecimal totalBancaUnits,
        BigDecimal profitLoss,
        BigDecimal profitLossUnits,
        List<BetHouseBalanceResponse> houses
) {
}
