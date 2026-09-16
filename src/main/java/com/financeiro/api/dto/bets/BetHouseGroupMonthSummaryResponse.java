package com.financeiro.api.dto.bets;

import java.math.BigDecimal;
import java.util.UUID;

/** Resumo do mês inteiro pra um grupo de casas (soma do resultado das casas que pertencem a esse grupo). */
public record BetHouseGroupMonthSummaryResponse(
        UUID groupId,
        String name,
        BigDecimal totalResult,
        BigDecimal totalResultUnits
) {
}
