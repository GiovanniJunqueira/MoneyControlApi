package com.financeiro.api.dto.bets;

import java.math.BigDecimal;
import java.util.UUID;

/** Banca ATUAL combinada de um agrupamento de casas - usada no overview (fora de um mês específico). */
public record BetGroupBancaResponse(UUID groupId, String name, BigDecimal banca, BigDecimal bancaUnits) {
}
