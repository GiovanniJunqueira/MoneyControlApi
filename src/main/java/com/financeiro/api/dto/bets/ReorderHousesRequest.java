package com.financeiro.api.dto.bets;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record ReorderHousesRequest(
        @NotEmpty(message = "Lista de casas obrigatória.") List<UUID> houseIds
) {
}
