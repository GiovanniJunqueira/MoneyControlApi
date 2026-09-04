package com.financeiro.api.dto.category;

import jakarta.validation.constraints.NotBlank;

public record CategoryRequest(
        @NotBlank(message = "Nome obrigatório.") String name,
        String color,
        String icon
) {
}
