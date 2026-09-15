package com.financeiro.api.dto.bets;

import java.math.BigDecimal;
import java.util.UUID;

public record BetHouseMonthSummaryResponse(
        UUID houseId,
        String name,
        String color,
        BigDecimal totalResult,
        BigDecimal totalResultUnits
) {
}
