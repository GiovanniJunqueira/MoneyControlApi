package com.financeiro.api.dto.bets;

import java.math.BigDecimal;
import java.util.UUID;

public record BetCompetitionRankingEntryResponse(
        int position,
        UUID userId,
        String userName,
        boolean isYou,
        /** true quando a pessoa não tem um mês registrado nesse período - profitLoss/Units ficam 0. */
        boolean hasData,
        BigDecimal profitLoss,
        BigDecimal profitLossUnits
) {
}
