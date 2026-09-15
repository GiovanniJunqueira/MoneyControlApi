package com.financeiro.api.dto.bets;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BetMonthSummaryResponse(
        UUID id,
        LocalDate startDate,
        LocalDate endDate,
        boolean open,
        BigDecimal startingBanca,
        BigDecimal endingBanca,
        BigDecimal profitLoss,
        BigDecimal profitLossUnits
) {
}
