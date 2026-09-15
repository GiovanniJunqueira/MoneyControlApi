package com.financeiro.api.dto.tab;

import jakarta.validation.constraints.NotBlank;

public record TabRequest(
        @NotBlank(message = "Nome obrigatório.") String name,
        String color
) {
}
