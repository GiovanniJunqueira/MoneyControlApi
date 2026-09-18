package com.financeiro.api.dto.bets;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record BetCompetitionRankingEntryResponse(
        int position,
        UUID userId,
        String userName,
        boolean isYou,
        /** true quando a pessoa não tem um mês registrado nesse período - profitLoss/Units ficam 0. */
        boolean hasData,
        BigDecimal profitLoss,
        BigDecimal profitLossUnits,
        /** Últimos 3 dias (mais recente primeiro), só o resultado total de cada dia - pra expandir
         * a linha da pessoa no ranking sem mostrar nada além disso (nem casa, nem R$ no front). */
        List<BetCompetitionDayResultResponse> recentDays
) {
}
