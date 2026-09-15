package com.financeiro.api.dto.bets;

import java.math.BigDecimal;
import java.util.UUID;

public record BetHouseBalanceResponse(
        UUID id,
        String name,
        String color,
        BigDecimal currentBalance,
        BigDecimal currentBalanceUnits,
        BigDecimal startOfDayBalance,
        boolean updatedToday
) {
}
