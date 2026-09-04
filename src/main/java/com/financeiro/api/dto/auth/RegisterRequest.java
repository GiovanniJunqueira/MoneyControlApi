package com.financeiro.api.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Nome obrigatório.") @Size(min = 2, message = "Nome muito curto.") String name,
        @NotBlank(message = "E-mail obrigatório.") @Email(message = "E-mail inválido.") String email,
        @NotBlank(message = "Senha obrigatória.") @Size(min = 6, message = "A senha precisa ter no mínimo 6 caracteres.") String password
) {
}
