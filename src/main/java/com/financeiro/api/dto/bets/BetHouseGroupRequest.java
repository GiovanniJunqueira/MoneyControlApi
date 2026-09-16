package com.financeiro.api.dto.bets;

import jakarta.validation.constraints.NotBlank;

public record BetHouseGroupRequest(
        @NotBlank(message = "Nome obrigatório.") String name
) {
}
