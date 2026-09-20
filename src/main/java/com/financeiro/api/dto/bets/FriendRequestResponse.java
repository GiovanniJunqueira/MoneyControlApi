package com.financeiro.api.dto.bets;

import java.time.LocalDateTime;
import java.util.UUID;

public record FriendRequestResponse(UUID id, UUID fromUserId, String fromUserName, LocalDateTime createdAt) {
}
