package com.financeiro.api.dto.bets;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record BetMonthDaysResponse(
        UUID monthId,
        LocalDate startDate,
        LocalDate endDate,
        boolean open,
        BigDecimal startingBanca,
        BigDecimal endingBanca,
        BigDecimal profitLoss,
        BigDecimal profitLossUnits,
        List<BetHouseMonthSummaryResponse> houseSummaries,
        List<BetHouseGroupMonthSummaryResponse> groupSummaries,
        List<BetMonthDayResponse> days
) {
}
