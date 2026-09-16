package com.financeiro.api.dto.bets;

import jakarta.validation.constraints.NotBlank;

public record JoinCompetitionRequest(
        @NotBlank(message = "Código obrigatório.") String code
) {
}
