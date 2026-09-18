package com.financeiro.api.dto.bets;

import java.time.LocalDate;
import java.util.UUID;

public record BetHouseResponse(UUID id, String name, String color, int position, UUID groupId, String groupName,
                                LocalDate archivedFrom) {
}
