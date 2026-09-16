package com.financeiro.api.dto.bets;

import java.math.BigDecimal;
import java.util.UUID;

public record BetMonthDayHouseResponse(
        UUID houseId,
        String name,
        String color,
        BigDecimal balance,
        BigDecimal balanceUnits,
        BigDecimal result,
        BigDecimal resultUnits,
        /** Ajuste manual do saldo inicial já salvo pra esse dia (null = automático/carry-forward). */
        BigDecimal openingOverride,
        UUID groupId,
        String groupName
) {
}
