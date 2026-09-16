package com.financeiro.api.dto.bets;

import java.util.List;
import java.util.UUID;

public record BetCompetitionRankingResponse(
        UUID id, String name, String code, int year, int month,
        List<BetCompetitionRankingEntryResponse> ranking
) {
}
