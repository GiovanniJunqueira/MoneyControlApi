package com.financeiro.api.dto.bets;

import jakarta.validation.constraints.NotBlank;

public record SendFriendRequestRequest(@NotBlank String code) {
}
