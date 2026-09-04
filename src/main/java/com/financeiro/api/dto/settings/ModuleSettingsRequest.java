package com.financeiro.api.dto.settings;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ModuleSettingsRequest(
        @NotNull(message = "Dia de fechamento obrigatório.")
        @Min(value = 1, message = "O dia de fechamento deve ser entre 1 e 28.")
        @Max(value = 28, message = "O dia de fechamento deve ser entre 1 e 28.")
        Integer closingDay
) {
}
