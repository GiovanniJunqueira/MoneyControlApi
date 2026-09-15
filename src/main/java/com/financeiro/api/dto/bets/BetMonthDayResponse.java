package com.financeiro.api.dto.bets;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record BetMonthDayResponse(
        LocalDate date,
        BigDecimal unitValue,
        BigDecimal total,
        BigDecimal totalUnits,
        BigDecimal result,
        BigDecimal resultUnits,
        List<BetMonthDayHouseResponse> houses
) {
}
