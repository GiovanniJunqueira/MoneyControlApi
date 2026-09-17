package com.financeiro.api.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
        @NotBlank(message = "E-mail obrigatório.") @Email(message = "E-mail inválido.") String email
) {
}
