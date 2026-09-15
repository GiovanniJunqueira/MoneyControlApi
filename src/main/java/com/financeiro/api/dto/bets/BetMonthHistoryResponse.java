package com.financeiro.api.dto.bets;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BetMonthHistoryResponse(
        UUID id,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal startingBanca,
        BigDecimal endingBanca,
        BigDecimal profitLoss
) {
}
