package com.financeiro.api.dto.bets;

import java.math.BigDecimal;
import java.util.UUID;

/** Subtotal do dia pra um grupo de casas (soma das casas que pertencem a esse grupo). */
public record BetMonthDayGroupResponse(
        UUID groupId,
        String name,
        BigDecimal total,
        BigDecimal totalUnits,
        BigDecimal result,
        BigDecimal resultUnits
) {
}
