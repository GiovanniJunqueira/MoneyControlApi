package com.financeiro.api.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Token obrigatório.") String token,
        @NotBlank(message = "Senha obrigatória.") @Size(min = 6, message = "A senha precisa ter no mínimo 6 caracteres.") String newPassword
) {
}
