package com.financeiro.api.dto.bets;

import java.util.UUID;

/** groupId nulo remove a casa do grupo em que ela estava. */
public record AssignHouseGroupRequest(UUID groupId) {
}
