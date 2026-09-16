package com.financeiro.api.dto.bets;

import java.util.UUID;

public record BetCompetitionResponse(UUID id, String name, String code, int year, int month, boolean isCreator) {
}
