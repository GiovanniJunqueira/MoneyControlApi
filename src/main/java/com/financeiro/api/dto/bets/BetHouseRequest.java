package com.financeiro.api.dto.bets;

import jakarta.validation.constraints.NotBlank;

public record BetHouseRequest(
        @NotBlank(message = "Nome obrigatório.") String name,
        String color
) {
}
