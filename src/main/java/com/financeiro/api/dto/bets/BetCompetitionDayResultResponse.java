package com.financeiro.api.dto.bets;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BetCompetitionDayResultResponse(LocalDate date, BigDecimal result, BigDecimal resultUnits) {
}
