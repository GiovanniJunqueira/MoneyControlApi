package com.financeiro.api.dto.auth;

import jakarta.validation.constraints.NotNull;

public record UpdateSettingsRequest(@NotNull(message = "Campo obrigatório.") Boolean betsEnabled) {
}
