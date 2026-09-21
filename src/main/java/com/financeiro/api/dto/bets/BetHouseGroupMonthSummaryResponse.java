package com.financeiro.api.dto.bets;

import java.math.BigDecimal;
import java.util.UUID;

/** Resumo do mês inteiro pra um grupo de casas: soma do resultado das casas que pertencem a esse
 * grupo, mais a banca ATUAL combinada delas (saldo de agora, não resultado do mês). */
public record BetHouseGroupMonthSummaryResponse(
        UUID groupId,
        String name,
        BigDecimal totalResult,
        BigDecimal totalResultUnits,
        BigDecimal banca,
        BigDecimal bancaUnits
) {
}
